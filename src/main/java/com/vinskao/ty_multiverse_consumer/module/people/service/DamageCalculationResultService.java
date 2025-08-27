package com.vinskao.ty_multiverse_consumer.module.people.service;

import com.vinskao.ty_multiverse_consumer.module.people.dao.DamageCalculationResultRepository;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.DamageCalculationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 傷害計算結果服務
 * 負責管理傷害計算結果的存儲和查詢
 */
@Service
public class DamageCalculationResultService {

    @Autowired
    private DamageCalculationResultRepository damageCalculationResultRepository;

    /**
     * 創建新的傷害計算請求記錄
     */
    @Transactional
    public DamageCalculationResult createCalculationRequest(String requestId, String characterName) {
        DamageCalculationResult result = new DamageCalculationResult(requestId, characterName);
        return damageCalculationResultRepository.save(result);
    }

    /**
     * 更新傷害計算結果為成功
     */
    @Transactional
    public void updateCalculationSuccess(String requestId, Integer damageValue) {
        Optional<DamageCalculationResult> optional = damageCalculationResultRepository.findByRequestId(requestId);
        if (optional.isPresent()) {
            DamageCalculationResult result = optional.get();
            result.setStatus("success");
            result.setDamageValue(damageValue);
            result.setCompletedAt(LocalDateTime.now());
            damageCalculationResultRepository.save(result);
        }
    }

    /**
     * 更新傷害計算結果為失敗
     */
    @Transactional
    public void updateCalculationError(String requestId, String errorMessage) {
        Optional<DamageCalculationResult> optional = damageCalculationResultRepository.findByRequestId(requestId);
        if (optional.isPresent()) {
            DamageCalculationResult result = optional.get();
            result.setStatus("error");
            result.setErrorMessage(errorMessage);
            result.setCompletedAt(LocalDateTime.now());
            damageCalculationResultRepository.save(result);
        }
    }

    /**
     * 根據請求ID查找傷害計算結果
     */
    public Optional<DamageCalculationResult> findByRequestId(String requestId) {
        return damageCalculationResultRepository.findByRequestId(requestId);
    }

    /**
     * 檢查請求ID是否存在
     */
    public boolean existsByRequestId(String requestId) {
        return damageCalculationResultRepository.existsByRequestId(requestId);
    }

    /**
     * 刪除過期的傷害計算結果（可選的清理功能）
     */
    @Transactional
    public void deleteExpiredResults(LocalDateTime before) {
        // 這裡可以實現刪除過期結果的邏輯
        // 例如刪除7天前的結果
    }
}
