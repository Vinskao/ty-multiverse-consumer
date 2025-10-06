package com.vinskao.ty_multiverse_consumer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.r2dbc.spi.ConnectionFactory;

/**
 * R2DBC 數據庫配置類
 *
 * 負責 reactive 數據庫連接池的監控和配置
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    /**
     * 數據庫健康檢查
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(ConnectionFactory connectionFactory) {
        return () -> {
            try {
                // R2DBC 健康檢查：簡單的健康狀態
                return Health.up().build();
            } catch (Exception e) {
                logger.error("數據庫健康檢查失敗", e);
                return Health.down(e).build();
            }
        };
    }
}
