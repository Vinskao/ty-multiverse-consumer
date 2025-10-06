package com.vinskao.ty_multiverse_consumer.module.people.dao;

import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.DamageCalculationResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 傷害計算結果Repository
 */
@Repository
public interface DamageCalculationResultRepository extends ReactiveCrudRepository<DamageCalculationResult, String> {

    /**
     * 根據請求ID查找傷害計算結果
     */
    Mono<DamageCalculationResult> findByRequestId(String requestId);

    /**
     * 檢查請求ID是否存在
     */
    Mono<Boolean> existsByRequestId(String requestId);
}
