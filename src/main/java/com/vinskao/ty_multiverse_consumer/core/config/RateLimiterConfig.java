package com.vinskao.ty_multiverse_consumer.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 限流配置類
 *
 * 引入 common 模組的 RateLimiterConfiguration
 * 啟用 @RateLimited 註解支援
 */
@Configuration
@Import(tw.com.ty.common.resilience.RateLimiterConfiguration.class)
public class RateLimiterConfig {
    // 限流配置由 common 模組的 RateLimiterConfiguration 處理
    // 可以在 Controller 方法上使用 @RateLimited 註解進行限流
}
