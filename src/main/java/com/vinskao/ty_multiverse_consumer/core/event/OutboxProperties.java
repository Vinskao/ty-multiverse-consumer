package com.vinskao.ty_multiverse_consumer.core.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Transactional Outbox 相關設定
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@ConfigurationProperties(prefix = "tymb.outbox")
public class OutboxProperties {

    /** 是否啟用 outbox 寫入與發布 */
    private boolean enabled = true;

    /** 輪詢間隔（毫秒） */
    private long pollIntervalMs = 2000;

    /** 每次輪詢最多發布幾筆 */
    private int batchSize = 100;

    /** 等待 RabbitMQ publisher confirm 的逾時（毫秒） */
    private long confirmTimeoutMs = 5000;

    /** 重試退避的基準（毫秒），實際退避為 base * 2^(attempt-1)，上限 maxBackoffMs */
    private long backoffBaseMs = 2000;

    /** 重試退避上限（毫秒） */
    private long maxBackoffMs = 300000;

    /** 超過這個嘗試次數時以 ERROR 等級告警（仍會繼續重試，事件不會被丟棄） */
    private int alertAfterAttempts = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getConfirmTimeoutMs() {
        return confirmTimeoutMs;
    }

    public void setConfirmTimeoutMs(long confirmTimeoutMs) {
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    public long getBackoffBaseMs() {
        return backoffBaseMs;
    }

    public void setBackoffBaseMs(long backoffBaseMs) {
        this.backoffBaseMs = backoffBaseMs;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }

    public int getAlertAfterAttempts() {
        return alertAfterAttempts;
    }

    public void setAlertAfterAttempts(int alertAfterAttempts) {
        this.alertAfterAttempts = alertAfterAttempts;
    }
}
