package com.vinskao.ty_multiverse_consumer.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tw.com.ty.common.event.BusinessEventType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional Outbox 的核心保證：業務寫入與 outbox 事件同生共死。
 *
 * <p>用真的 R2DBC transaction manager（H2 記憶體資料庫）驗證，
 * 而不是 mock —— 這條保證正是 mock 最容易假裝成立的地方。</p>
 */
class BusinessEventTransactionRollbackTest {

    private ConnectionFactory connectionFactory;
    private DatabaseClient databaseClient;
    private R2dbcTransactionManager transactionManager;
    private BusinessEventOutboxRepository outboxRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        String db = "rollback-" + UUID.randomUUID();
        connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///" + db + "?options=DB_CLOSE_DELAY=-1");
        databaseClient = DatabaseClient.create(connectionFactory);
        transactionManager = new R2dbcTransactionManager(connectionFactory);
        outboxRepository = new H2OutboxRepository(databaseClient);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 業務表：模擬 people
        exec("CREATE TABLE business_row (name VARCHAR(255) PRIMARY KEY, age INT)");
        // outbox 表：欄位對齊 db/business_event_outbox.sql，型別改用 H2 支援的形式
        exec("""
                CREATE TABLE business_event_outbox (
                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    event_id UUID NOT NULL UNIQUE,
                    event_type VARCHAR(128) NOT NULL,
                    aggregate_type VARCHAR(64) NOT NULL,
                    aggregate_id VARCHAR(255),
                    request_id VARCHAR(128),
                    payload CLOB NOT NULL,
                    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    published_at TIMESTAMP WITH TIME ZONE,
                    attempt_count INT NOT NULL DEFAULT 0,
                    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_error CLOB,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void exec(String sql) {
        databaseClient.sql(sql).fetch().rowsUpdated().block();
    }

    private long countBusinessRows() {
        return databaseClient.sql("SELECT count(*) FROM business_row")
                .map(row -> row.get(0, Long.class)).one().block();
    }

    private long countOutboxRows() {
        return databaseClient.sql("SELECT count(*) FROM business_event_outbox")
                .map(row -> row.get(0, Long.class)).one().block();
    }

    private Mono<String> businessWrite() {
        return databaseClient.sql("INSERT INTO business_row (name, age) VALUES ('Alice', 30)")
                .fetch().rowsUpdated().thenReturn("Alice");
    }

    @Test
    @DisplayName("事件寫入失敗時，業務寫入必須一併回滾")
    void eventFailureRollsBackBusinessWrite() {
        // 一個一定會序列化失敗的 mapper，模擬事件寫入端出錯
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonProcessingException("boom") {
                };
            }
        };
        BusinessEventRecorder recorder = new BusinessEventRecorder(outboxRepository, failing, new OutboxProperties());
        BusinessEventTransaction tx = new BusinessEventTransaction(transactionManager, recorder);

        StepVerifier.create(tx.withSucceededEvent(
                        businessWrite(),
                        BusinessEventType.PEOPLE_INSERT,
                        BusinessEventType.AGGREGATE_PEOPLE,
                        name -> name,
                        name -> Map.of("name", name),
                        "req-rollback"))
                .expectError(BusinessEventRecorder.BusinessEventSerializationException.class)
                .verify();

        assertThat(countBusinessRows())
                .as("事件寫不進去時，業務資料不能留下")
                .isZero();
        assertThat(countOutboxRows()).isZero();
    }

    @Test
    @DisplayName("業務寫入與事件同時成功時，兩邊都要留下")
    void bothCommitTogether() {
        BusinessEventRecorder recorder =
                new BusinessEventRecorder(outboxRepository, objectMapper, new OutboxProperties());
        BusinessEventTransaction tx = new BusinessEventTransaction(transactionManager, recorder);

        StepVerifier.create(tx.withSucceededEvent(
                        businessWrite(),
                        BusinessEventType.PEOPLE_INSERT,
                        BusinessEventType.AGGREGATE_PEOPLE,
                        name -> name,
                        name -> Map.of("name", name),
                        "req-commit"))
                .expectNext("Alice")
                .verifyComplete();

        assertThat(countBusinessRows()).isEqualTo(1);
        assertThat(countOutboxRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("業務寫入失敗時，不應留下 outbox 事件")
    void businessFailureLeavesNoEvent() {
        BusinessEventRecorder recorder =
                new BusinessEventRecorder(outboxRepository, objectMapper, new OutboxProperties());
        BusinessEventTransaction tx = new BusinessEventTransaction(transactionManager, recorder);

        StepVerifier.create(tx.withSucceededEvent(
                        Mono.<String>error(new IllegalStateException("business failed")),
                        BusinessEventType.PEOPLE_INSERT,
                        BusinessEventType.AGGREGATE_PEOPLE,
                        name -> name,
                        name -> Map.of("name", name),
                        "req-business-fail"))
                .expectErrorMessage("business failed")
                .verify();

        assertThat(countOutboxRows()).isZero();
    }

    @Test
    @DisplayName("outbox 寫入本身失敗時，業務寫入必須一併回滾")
    void outboxInsertFailureRollsBackBusinessWrite() {
        // 模擬 outbox 寫入出錯（例如 DB 短暫故障）
        BusinessEventOutboxRepository failingRepo = new BusinessEventOutboxRepository(databaseClient) {
            @Override
            public Mono<Long> append(String eventId, String eventType, String aggregateType, String aggregateId,
                                     String requestId, String payloadJson, java.time.Instant occurredAt) {
                return Mono.error(new IllegalStateException("outbox insert failed"));
            }
        };
        BusinessEventRecorder recorder =
                new BusinessEventRecorder(failingRepo, objectMapper, new OutboxProperties());
        BusinessEventTransaction tx = new BusinessEventTransaction(transactionManager, recorder);

        StepVerifier.create(tx.withSucceededEvent(
                        businessWrite(),
                        BusinessEventType.PEOPLE_INSERT,
                        BusinessEventType.AGGREGATE_PEOPLE,
                        name -> name,
                        name -> Map.of("name", name),
                        "req-outbox-fail"))
                .expectErrorMessage("outbox insert failed")
                .verify();

        assertThat(countBusinessRows())
                .as("outbox 寫不進去時，業務資料不能留下")
                .isZero();
    }

    /**
     * H2 版 outbox repository。
     *
     * <p>正式 SQL 用了 PostgreSQL 專屬的 {@code CAST(... AS jsonb)} 與
     * {@code ON CONFLICT DO NOTHING}，H2 兩者都不支援。這裡只覆寫 append 的 SQL 方言，
     * 其餘行為（同一個 DatabaseClient、同一個 connection、同一個 transaction）不變，
     * 因此仍能驗證 commit／rollback 的交易語意。</p>
     *
     * <p>{@code ON CONFLICT} 的去重行為屬於 PostgreSQL 專屬語意，改由
     * {@code verify-outbox-resilience.sh} 對正式資料庫驗證（該腳本會斷言同一個
     * eventId 在 outbox 中只有一筆）。</p>
     */
    private static final class H2OutboxRepository extends BusinessEventOutboxRepository {

        private final DatabaseClient client;

        private H2OutboxRepository(DatabaseClient client) {
            super(client);
            this.client = client;
        }

        @Override
        public Mono<Long> append(String eventId, String eventType, String aggregateType, String aggregateId,
                                 String requestId, String payloadJson, java.time.Instant occurredAt) {
            return client.sql("""
                            INSERT INTO business_event_outbox
                                (event_id, event_type, aggregate_type, aggregate_id, request_id, payload, occurred_at)
                            VALUES (:eventId, :eventType, :aggregateType, :aggregateId, :requestId,
                                    :payload, :occurredAt)
                            """)
                    .bind("eventId", UUID.fromString(eventId))
                    .bind("eventType", eventType)
                    .bind("aggregateType", aggregateType)
                    .bind("aggregateId", aggregateId == null ? "" : aggregateId)
                    .bind("requestId", requestId == null ? "" : requestId)
                    .bind("payload", payloadJson)
                    .bind("occurredAt", occurredAt)
                    .fetch()
                    .rowsUpdated();
        }
    }
}
