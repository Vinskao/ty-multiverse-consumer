package com.vinskao.ty_multiverse_consumer.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Common 模組配置匯入類
 *
 * 匯入並配置 common 模組中的所有必要組件：
 * - Exception handling (異常處理)
 * - Logging (日誌)
 * - Resilience (限流)
 * - Response (響應格式)
 * - Transaction (事務)
 */
@Configuration
@Import({
    // Exception handling - WebFlux 使用自己的異常處理器
    // tw.com.ty.common.exception.advice.GlobalExceptionHandler.class (Web MVC only)

    // Response - API 響應格式統一
    // (自動掃描使用 @RestController 的類別)

    // Logging - 請求響應日誌 AOP
    // (通過 AopConfig 啟用)

    // Resilience - 限流和重試配置
    tw.com.ty.common.resilience.RateLimiterConfiguration.class,
    tw.com.ty.common.resilience.RetryConfiguration.class

    // Transaction - R2DBC 不使用 JDBC 事務管理器，移除此項
    // tw.com.ty.common.transaction.config.TyTransactionConfig.class
})
public class CommonConfig {
    /*
     * 此配置類確保 Consumer 完整使用 common 模組的所有功能：
     *
     * 1. Exception Handling:
     *    - 使用 Consumer 自有的 WebFlux GlobalExceptionHandler
     *    - BusinessException, ErrorCode, ErrorResponse: 統一錯誤格式
     *    - (common 的 GlobalExceptionHandler 僅適用 Web MVC)
     *
     * 2. Logging:
     *    - RequestResponseLoggingAspect: 自動記錄請求響應日誌
     *    - 通過 AopConfig 啟用
     *
     * 3. Resilience:
     *    - RateLimiterConfiguration: 限流配置 (@RateLimited)
     *    - RetryConfiguration: 重試配置 (@Retryable)
     *    - RetryAspect: 重試切面處理
     *
     * 4. Response:
     *    - ApiResponse, BackendApiResponse: 統一響應格式
     *    - ErrorResponse, GatewayErrorResponse: 統一錯誤響應
     *
     * 5. Transaction:
     *    - R2DBC 不使用 JDBC 事務管理，移除 TyTransactionConfig
     */
}
