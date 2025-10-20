package com.vinskao.ty_multiverse_consumer.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 配置類
 *
 * 啟用 AspectJ AOP 支援，讓 common 模組中的 AOP 類別能夠生效
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // AOP 會自動掃描並應用 @Aspect 註解的類別，包括 common 模組中的類別
}
