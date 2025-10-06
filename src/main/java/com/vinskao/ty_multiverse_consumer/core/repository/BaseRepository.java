package com.vinskao.ty_multiverse_consumer.core.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Base repository interface that defines common methods for all repository types.
 * This serves as the foundation for creating specific repository types.
 *
 * @param <T> The entity type
 * @param <ID> The ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends ReactiveCrudRepository<T, ID> {
    // Common methods for all repository types can be added here
    // Note: R2DBC doesn't support Specification, so removed JpaSpecificationExecutor
} 