package com.vinskao.ty_multiverse_consumer.core.event;

import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * business_event_outbox 存取層
 *
 * <p>刻意使用 {@link DatabaseClient} 直接下 SQL，而不是 R2DBC entity mapping：
 * payload 欄位是 jsonb，用明確的 {@code ::jsonb} cast 最直接，
 * 也讓 append 能參與呼叫端既有的 transaction。</p>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Repository
public class BusinessEventOutboxRepository {

    private static final String SQL_APPEND = """
            INSERT INTO business_event_outbox
                (event_id, event_type, aggregate_type, aggregate_id, request_id, payload, occurred_at)
            VALUES
                (CAST(:eventId AS uuid), :eventType, :aggregateType, :aggregateId, :requestId,
                 CAST(:payload AS jsonb), :occurredAt)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String SQL_FIND_DUE = """
            SELECT id, event_id, event_type, payload, attempt_count
            FROM business_event_outbox
            WHERE published_at IS NULL
              AND next_attempt_at <= now()
            ORDER BY occurred_at, id
            LIMIT :limit
            """;

    private static final String SQL_MARK_PUBLISHED = """
            UPDATE business_event_outbox
            SET published_at = now(),
                attempt_count = attempt_count + 1,
                last_error = NULL
            WHERE id = :id
              AND published_at IS NULL
            """;

    private static final String SQL_MARK_FAILED = """
            UPDATE business_event_outbox
            SET attempt_count = attempt_count + 1,
                last_error = :lastError,
                next_attempt_at = now() + make_interval(secs => :backoffSeconds)
            WHERE id = :id
              AND published_at IS NULL
            """;

    private static final String SQL_COUNT_PENDING = """
            SELECT count(*) FROM business_event_outbox WHERE published_at IS NULL
            """;

    private final DatabaseClient databaseClient;

    public BusinessEventOutboxRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    /**
     * 寫入一筆待發布事件。
     *
     * <p>必須與業務資料的寫入在同一個 transaction 內執行，
     * 否則就退化成「DB 寫一次、MQ 再寫一次」的不一致做法。</p>
     *
     * <p>event_id 有唯一索引且使用 ON CONFLICT DO NOTHING，
     * 因此同一個事件重複寫入不會產生第二筆。</p>
     *
     * @return 實際插入的列數（0 代表該 eventId 已存在）
     */
    public Mono<Long> append(String eventId,
                             String eventType,
                             String aggregateType,
                             String aggregateId,
                             String requestId,
                             String payloadJson,
                             Instant occurredAt) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(SQL_APPEND)
                .bind("eventId", eventId)
                .bind("eventType", eventType)
                .bind("aggregateType", aggregateType)
                .bind("payload", payloadJson)
                .bind("occurredAt", occurredAt);
        spec = bindNullable(spec, "aggregateId", aggregateId);
        spec = bindNullable(spec, "requestId", requestId);
        return spec.fetch().rowsUpdated();
    }

    /**
     * 撈出到期且尚未發布的事件
     */
    public Flux<PendingEvent> findDue(int limit) {
        return databaseClient.sql(SQL_FIND_DUE)
                .bind("limit", limit)
                .map(BusinessEventOutboxRepository::mapPending)
                .all();
    }

    /**
     * 標記為已發布（RabbitMQ confirm ack 之後才呼叫）
     */
    public Mono<Long> markPublished(long id) {
        return databaseClient.sql(SQL_MARK_PUBLISHED)
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    /**
     * 標記本次發布失敗，並把下次嘗試時間往後推。事件本身保留，不會被丟棄。
     */
    public Mono<Long> markFailed(long id, String lastError, long backoffSeconds) {
        return databaseClient.sql(SQL_MARK_FAILED)
                .bind("id", id)
                .bind("lastError", lastError == null ? "unknown error" : lastError)
                .bind("backoffSeconds", backoffSeconds)
                .fetch()
                .rowsUpdated();
    }

    /**
     * 目前尚未發布的事件數，供監控 backlog 使用
     */
    public Mono<Long> countPending() {
        return databaseClient.sql(SQL_COUNT_PENDING)
                .map(row -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec,
                                                                 String name,
                                                                 String value) {
        return value == null ? spec.bindNull(name, String.class) : spec.bind(name, value);
    }

    private static PendingEvent mapPending(Readable row) {
        return new PendingEvent(
                row.get("id", Long.class),
                row.get("event_id", Object.class).toString(),
                row.get("event_type", String.class),
                row.get("payload", String.class),
                row.get("attempt_count", Integer.class));
    }

    /**
     * publisher 需要的最小欄位集合
     *
     * @param id           outbox 主鍵
     * @param eventId      事件 UUID，同時作為 AMQP message id 供下游去重
     * @param eventType    事件型別，例如 people.update.succeeded
     * @param payloadJson  完整的 BusinessEvent JSON，原樣送出
     * @param attemptCount 已嘗試次數
     */
    public record PendingEvent(Long id, String eventId, String eventType, String payloadJson, Integer attemptCount) {
    }
}
