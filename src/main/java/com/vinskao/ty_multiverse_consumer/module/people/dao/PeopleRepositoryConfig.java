package com.vinskao.ty_multiverse_consumer.module.people.dao;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Configuration for People repositories (R2DBC)
 * 配置 R2DBC reactive repositories
 */
@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.vinskao.ty_multiverse_consumer.module.people.dao"
)
public class PeopleRepositoryConfig {
    // Spring Data R2DBC will automatically create repository implementations
}
