package com.vinskao.ty_multiverse_consumer.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tw.com.ty.common.event.BusinessEvent;
import tw.com.ty.common.event.BusinessEventType;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BusinessEventRecorder} 的錯誤傳遞行為
 *
 * <p>核心不變條件：成功事件的寫入失敗（含序列化失敗）必須往外傳，
 * 否則業務 transaction 會 commit 但 outbox 沒有紀錄，
 * transactional outbox 的保證就破了。</p>
 */
class BusinessEventRecorderTest {

    private BusinessEventOutboxRepository outboxRepository;
    private OutboxProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(BusinessEventOutboxRepository.class);
        // 與 Spring Boot 自動組態的 ObjectMapper 等價：outbox 的 occurredAt 是 Instant，
        // 沒有 JavaTimeModule 就序列化不了。應用程式注入的是 Boot 那顆，已含此模組。
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        properties = new OutboxProperties();
    }

    private BusinessEvent sampleEvent() {
        return BusinessEvent.builder()
                .eventType(BusinessEventType.succeeded(BusinessEventType.PEOPLE_UPDATE))
                .aggregateType(BusinessEventType.AGGREGATE_PEOPLE)
                .aggregateId("Alice")
                .requestId("req-1")
                .occurredAt(Instant.parse("2026-08-26T10:30:00Z"))
                .payload(Map.of("changedFields", java.util.List.of("age")))
                .build();
    }

    @Test
    @DisplayName("序列化失敗必須往外傳，不能吞掉")
    void serializationFailurePropagates() {
        ObjectMapper failing = mock(ObjectMapper.class);
        try {
            when(failing.writeValueAsString(any()))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {
                    });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, failing, properties);

        StepVerifier.create(recorder.record(sampleEvent()))
                .expectError(BusinessEventRecorder.BusinessEventSerializationException.class)
                .verify();

        // 序列化失敗時不應該送出任何 outbox 寫入
        verify(outboxRepository, never())
                .append(anyString(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("outbox 寫入失敗必須往外傳，讓業務 transaction 回滾")
    void appendFailurePropagates() {
        when(outboxRepository.append(anyString(), anyString(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(Mono.error(new IllegalStateException("db down")));
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, objectMapper, properties);

        StepVerifier.create(recorder.record(sampleEvent()))
                .expectErrorMessage("db down")
                .verify();
    }

    @Test
    @DisplayName("關閉 outbox 時不寫入也不報錯")
    void disabledSkipsWrite() {
        properties.setEnabled(false);
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, objectMapper, properties);

        StepVerifier.create(recorder.record(sampleEvent())).verifyComplete();

        verify(outboxRepository, never())
                .append(anyString(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("成功路徑寫入的欄位與 payload JSON 正確")
    void happyPathWritesExpectedColumns() throws Exception {
        when(outboxRepository.append(anyString(), anyString(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(Mono.just(1L));
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, objectMapper, properties);
        BusinessEvent event = sampleEvent();

        StepVerifier.create(recorder.record(event)).verifyComplete();

        org.mockito.ArgumentCaptor<String> payloadCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).append(
                eq(event.getEventId()),
                eq("people.update.succeeded"),
                eq("people"),
                eq("Alice"),
                eq("req-1"),
                payloadCaptor.capture(),
                eq(event.getOccurredAt()));

        // 存進 outbox 的 JSON 必須能還原成同一個事件，publisher 會原樣送出這段內容
        BusinessEvent roundTrip = objectMapper.readValue(payloadCaptor.getValue(), BusinessEvent.class);
        assertThat(roundTrip.getEventId()).isEqualTo(event.getEventId());
        assertThat(roundTrip.getEventType()).isEqualTo("people.update.succeeded");
        assertThat(roundTrip.getRequestId()).isEqualTo("req-1");
        assertThat(roundTrip.getSchemaVersion()).isEqualTo(BusinessEvent.CURRENT_SCHEMA_VERSION);
        assertThat(roundTrip.routingKey()).isEqualTo("event.people.update.succeeded");
    }

    @Test
    @DisplayName("failed 事件只保留錯誤代碼與訊息首行，不含 stack trace")
    void failedEventKeepsOnlyErrorSummary() throws Exception {
        when(outboxRepository.append(anyString(), anyString(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(Mono.just(1L));
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, objectMapper, properties);

        StepVerifier.create(recorder.recordFailed(
                BusinessEventType.PEOPLE_INSERT,
                BusinessEventType.AGGREGATE_PEOPLE,
                null,
                "req-2",
                new IllegalArgumentException("bad payload\n\tat some.Frame(Foo.java:1)")))
                .verifyComplete();

        org.mockito.ArgumentCaptor<String> payloadCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).append(
                anyString(), eq("people.insert.failed"), eq("people"), any(), eq("req-2"),
                payloadCaptor.capture(), any());

        BusinessEvent roundTrip = objectMapper.readValue(payloadCaptor.getValue(), BusinessEvent.class);
        assertThat(roundTrip.getError().getCode()).isEqualTo("IllegalArgumentException");
        assertThat(roundTrip.getError().getMessage()).isEqualTo("bad payload");
        assertThat(payloadCaptor.getValue()).doesNotContain("some.Frame");
    }
}
