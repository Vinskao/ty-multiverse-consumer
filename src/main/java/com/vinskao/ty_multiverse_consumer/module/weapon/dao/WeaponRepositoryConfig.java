package com.vinskao.ty_multiverse_consumer.module.weapon.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 專門配置 weapon 模組的 repository (已禁用)
 * 使用條件註解禁用此配置，保留配置備用
 */
@Configuration
@ConditionalOnProperty(name = "spring.people-datasource.enabled", havingValue = "true")
@EnableJpaRepositories(
    basePackages = "com.vinskao.ty_multiverse_consumer.module.weapon.dao",
    entityManagerFactoryRef = "peopleEntityManagerFactory",
    transactionManagerRef = "peopleTransactionManager",
    repositoryImplementationPostfix = "Impl",
    repositoryBaseClass = com.vinskao.ty_multiverse_consumer.core.repository.StringPkRepositoryImpl.class
)
public class WeaponRepositoryConfig {
    // 使用 StringPkRepositoryImpl 作為基礎實現 (已禁用)
}
