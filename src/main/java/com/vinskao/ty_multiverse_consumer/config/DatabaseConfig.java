package com.vinskao.ty_multiverse_consumer.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

/**
 * 數據庫配置類
 * 
 * 負責數據庫連接池的監控和配置
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 數據庫健康檢查
     */
    @Bean
    public HealthIndicator databaseHealthIndicator() {
        return new HealthIndicator() {
            @Override
            public Health health() {
                try {
                    if (dataSource instanceof HikariDataSource) {
                        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                        Health.Builder builder = Health.up();
                        
                        // 檢查 MXBean 是否可用
                        if (hikariDataSource.getHikariPoolMXBean() != null) {
                            builder.withDetail("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections())
                                   .withDetail("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections())
                                   .withDetail("totalConnections", hikariDataSource.getHikariPoolMXBean().getTotalConnections())
                                   .withDetail("threadsAwaitingConnection", hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                        } else {
                            builder.withDetail("status", "MXBean not available");
                        }
                        
                        return builder.build();
                    }
                    return Health.up().build();
                } catch (Exception e) {
                    logger.error("數據庫健康檢查失敗", e);
                    return Health.down(e).build();
                }
            }
        };
    }

    /**
     * 定期監控連接池狀態
     */
    @Scheduled(fixedRate = 30000) // 每30秒執行一次
    public void monitorConnectionPool() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            try {
                // 檢查 MXBean 是否可用
                if (hikariDataSource.getHikariPoolMXBean() != null) {
                    int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                    int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
                    int totalConnections = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
                    int threadsAwaitingConnection = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

                    logger.info("連接池狀態 - 活躍: {}, 空閒: {}, 總數: {}, 等待: {}", 
                               activeConnections, idleConnections, totalConnections, threadsAwaitingConnection);

                    // 如果活躍連接數過高，記錄警告
                    if (activeConnections > 0) {
                        logger.warn("檢測到活躍連接: {}", activeConnections);
                    }
                } else {
                    logger.debug("HikariPoolMXBean 不可用，跳過連接池監控");
                }
            } catch (Exception e) {
                logger.error("監控連接池狀態失敗", e);
            }
        }
    }
}
