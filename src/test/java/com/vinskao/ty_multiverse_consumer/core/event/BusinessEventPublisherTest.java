package com.vinskao.ty_multiverse_consumer.core.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BusinessEventPublisher} 的 confirm 處理與重試行為
 *
 * <p>核心不變條件：只有收到 broker 的 ack 才可以標記為已發布。
 * nack、逾時、unroutable 都必須保留事件並安排重試。</p>
 */
class BusinessEventPublisherTest {

    private BusinessEventOutboxRepository outboxRepository;
    private RabbitTemplate rabbitTemplate;
    private OutboxProperties properties;
    private BusinessEventPublisher publisher;

    private static final String EVENT_ID = "3f1d6f6e-0f4b-4a2f-9f6a-2a1b3c4d5e6f";
    private static final String PAYLOAD = "{\"eventId\":\"" + EVENT_ID + "\"}";

    @BeforeEach
    void setUp() {
        outboxRepository = mock(BusinessEventOutboxRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        properties = new OutboxProperties();
        properties.setConfirmTimeoutMs(1000);
        publisher = new BusinessEventPublisher(outboxRepository, rabbitTemplate, properties);

        when(outboxRepository.markPublished(anyLong())).thenReturn(Mono.just(1L));
        when(outboxRepository.markFailed(anyLong(), anyString(), anyLong())).thenReturn(Mono.just(1L));
    }

    private void givenPending(int attemptCount) {
        when(outboxRepository.findDue(properties.getBatchSize()))
                .thenReturn(Flux.just(new BusinessEventOutboxRepository.PendingEvent(
                        7L, EVENT_ID, "people.update.succeeded", PAYLOAD, attemptCount)));
    }

    /** 讓 rabbitTemplate.send 以指定結果完成 confirm future */
    private void stubConfirm(boolean ack, String reason) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("ack 之後才標記為已發布，且送出的訊息符合規格")
    void ackMarksPublished() {
        givenPending(0);
        stubConfirm(true, null);

        publisher.publishPending();

        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(EventRabbitMQConfig.TYMB_EVENT_EXCHANGE),
                routingKey.capture(),
                message.capture(),
                any(CorrelationData.class));

        // routing key 必須帶 event. 前綴才會被 event.# binding 收到
        assertThat(routingKey.getValue()).isEqualTo("event.people.update.succeeded");

        MessageProperties props = message.getValue().getMessageProperties();
        // messageId = eventId，下游靠它做冪等去重
        assertThat(props.getMessageId()).isEqualTo(EVENT_ID);
        assertThat(props.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(props.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        // outbox 裡存什麼就送什麼，稽核內容與發布內容一致
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8)).isEqualTo(PAYLOAD);

        verify(outboxRepository).markPublished(7L);
        verify(outboxRepository, never()).markFailed(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("nack 不可標記為已發布，必須保留並重試")
    void nackKeepsEventForRetry() {
        givenPending(0);
        stubConfirm(false, "broker rejected");

        publisher.publishPending();

        verify(outboxRepository, never()).markPublished(anyLong());
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).markFailed(eq(7L), reason.capture(), anyLong());
        assertThat(reason.getValue()).contains("nack").contains("broker rejected");
    }

    @Test
    @DisplayName("RabbitMQ 中斷（send 直接拋錯）時保留事件並重試")
    void brokerDownKeepsEventForRetry() {
        givenPending(0);
        doAnswer(invocation -> {
            throw new org.springframework.amqp.AmqpConnectException(new RuntimeException("connection refused"));
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPending();

        verify(outboxRepository, never()).markPublished(anyLong());
        verify(outboxRepository).markFailed(eq(7L), anyString(), anyLong());
    }

    @Test
    @DisplayName("confirm 逾時不可標記為已發布")
    void confirmTimeoutKeepsEventForRetry() {
        givenPending(0);
        properties.setConfirmTimeoutMs(50);
        // 不完成 future，模擬 broker 沒有回 confirm
        doAnswer(invocation -> null).when(rabbitTemplate)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPending();

        verify(outboxRepository, never()).markPublished(anyLong());
        verify(outboxRepository).markFailed(eq(7L), anyString(), anyLong());
    }

    @Test
    @DisplayName("退避時間隨嘗試次數指數成長並受上限約束")
    void backoffGrowsExponentiallyAndIsCapped() {
        properties.setBackoffBaseMs(2000);
        properties.setMaxBackoffMs(300000);
        stubConfirm(false, "nope");

        // 第 1 次失敗（attemptCount 0 -> 1）：2s
        givenPending(0);
        publisher.publishPending();
        verify(outboxRepository).markFailed(eq(7L), anyString(), eq(2L));

        // 第 4 次失敗（attemptCount 3 -> 4）：2s * 2^3 = 16s
        givenPending(3);
        publisher.publishPending();
        verify(outboxRepository).markFailed(eq(7L), anyString(), eq(16L));

        // 高嘗試次數必須被 maxBackoffMs 夾住，不會溢位或無限成長
        givenPending(50);
        publisher.publishPending();
        verify(outboxRepository).markFailed(eq(7L), anyString(), eq(300L));
    }

    @Test
    @DisplayName("讀取 outbox 失敗時排程不應中斷")
    void repositoryFailureDoesNotKillScheduler() {
        when(outboxRepository.findDue(properties.getBatchSize()))
                .thenReturn(Flux.error(new IllegalStateException("db down")));

        publisher.publishPending();

        verify(rabbitTemplate, never())
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }
}
