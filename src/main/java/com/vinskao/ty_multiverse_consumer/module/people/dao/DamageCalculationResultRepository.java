package com.vinskao.ty_multiverse_consumer.module.people.dao;

import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.DamageCalculationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 傷害計算結果Repository
 */
@Repository
public interface DamageCalculationResultRepository extends JpaRepository<DamageCalculationResult, String> {
    
    /**
     * 根據請求ID查找傷害計算結果
     */
    Optional<DamageCalculationResult> findByRequestId(String requestId);
    
    /**
     * 檢查請求ID是否存在
     */
    boolean existsByRequestId(String requestId);
}
