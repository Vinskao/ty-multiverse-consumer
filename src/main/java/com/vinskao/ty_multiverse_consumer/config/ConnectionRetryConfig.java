package com.vinskao.ty_multiverse_consumer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 連接重試配置
 * 
 * 處理數據庫連接失敗時的重試機制
 */
@Configuration
@EnableRetry
public class ConnectionRetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionRetryConfig.class);

    /**
     * 數據庫操作重試模板
     */
    @Bean
    public RetryTemplate databaseRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 指數退避策略 - 更長的等待時間
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(5000); // 5秒
        backOffPolicy.setMultiplier(2.0); // 每次重試間隔翻倍
        backOffPolicy.setMaxInterval(60000); // 最大間隔60秒
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // 重試策略 - 更多重試次數
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(org.springframework.transaction.CannotCreateTransactionException.class, true);
        retryableExceptions.put(io.r2dbc.spi.R2dbcException.class, true);
        retryableExceptions.put(java.sql.SQLTransientConnectionException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(10, retryableExceptions, true); // 增加到10次重試
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
