package com.vinskao.ty_multiverse_consumer.module.weapon.dao;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * 專門配置 weapon 模組的 repository (R2DBC)
 * 配置 R2DBC reactive repositories
 */
@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.vinskao.ty_multiverse_consumer.module.weapon.dao"
)
public class WeaponRepositoryConfig {
    // Spring Data R2DBC will automatically create repository implementations
}
