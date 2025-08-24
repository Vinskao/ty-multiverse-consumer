package com.vinskao.ty_multiverse_consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 數據庫連接服務
 * 
 * 提供智能的數據庫連接等待和重試機制
 */
@Service
public class DatabaseConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 等待數據庫連接可用
     * 
     * @param maxWaitSeconds 最大等待秒數
     * @return 是否成功獲取連接
     */
    @Retryable(
        value = {SQLException.class},
        maxAttempts = 20,
        backoff = @Backoff(delay = 5000, multiplier = 1.5, maxDelay = 60000)
    )
    public boolean waitForConnection(int maxWaitSeconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (maxWaitSeconds * 1000L);
        
        while (System.currentTimeMillis() < endTime) {
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    logger.info("成功獲取數據庫連接，等待時間: {} 秒", 
                               (System.currentTimeMillis() - startTime) / 1000);
                    return true;
                }
            } catch (SQLException e) {
                long remainingTime = (endTime - System.currentTimeMillis()) / 1000;
                logger.warn("無法獲取數據庫連接，剩餘等待時間: {} 秒, 錯誤: {}", remainingTime, e.getMessage());
                
                if (remainingTime <= 0) {
                    logger.error("等待數據庫連接超時");
                    throw new RuntimeException("數據庫連接等待超時", e);
                }
                
                try {
                    // 等待5秒後重試
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待被中斷", ie);
                }
            }
        }
        
        logger.error("等待數據庫連接超時");
        return false;
    }

    /**
     * 檢查數據庫連接狀態
     * 
     * @return 連接是否可用
     */
    public boolean isConnectionAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.warn("數據庫連接檢查失敗: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 強制等待連接可用
     * 
     * @return 是否成功
     */
    public boolean forceWaitForConnection() {
        logger.info("開始強制等待數據庫連接...");
        return waitForConnection(300); // 等待5分鐘
    }
}

