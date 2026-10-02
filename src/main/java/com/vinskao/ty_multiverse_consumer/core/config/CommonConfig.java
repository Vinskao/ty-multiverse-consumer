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
    // Exception handling - 由 common 2.3.0 的 CommonExceptionAutoConfiguration 自動註冊
    // (WebFlux 會得到 ReactiveExceptionAdvice，不需要在這裡 @Import)

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
     *    - common 的 ReactiveExceptionAdvice（自動註冊，見 common/docs/ERROR_HANDLING.md）
     *    - BusinessException, ErrorCode, ErrorResponse: 統一錯誤格式
     *    - 不要再自行宣告 @ExceptionHandler(Exception.class) 的 advice，兩個 catch-all 會讓結果不確定
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
