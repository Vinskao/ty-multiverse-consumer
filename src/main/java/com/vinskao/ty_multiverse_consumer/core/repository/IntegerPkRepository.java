package com.vinskao.ty_multiverse_consumer.core.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository implementation for entities that use Integer as their primary key.
 * This interface extends the base repository and adds Integer-specific methods.
 *
 * @param <T> The entity type
 */
@NoRepositoryBean
public interface IntegerPkRepository<T> extends BaseRepository<T, Integer> {

    /**
     * Find entity by ID
     *
     * @param id The ID to search for
     * @return Mono containing the entity if found
     */
    @NonNull Mono<T> findById(@NonNull Integer id);

    /**
     * Check if an entity exists by ID
     *
     * @param id The ID to check
     * @return Mono emitting true if the entity exists, false otherwise
     */
    Mono<Boolean> existsById(@NonNull Integer id);

    /**
     * Delete an entity by ID
     *
     * @param id The ID of the entity to delete
     * @return Mono<Void>
     */
    Mono<Void> deleteById(@NonNull Integer id);

    /**
     * Find all entities
     * @return Flux of all entities
     */
    @NonNull Flux<T> findAll();

    // Note: R2DBC doesn't support Specification and Pageable
} 