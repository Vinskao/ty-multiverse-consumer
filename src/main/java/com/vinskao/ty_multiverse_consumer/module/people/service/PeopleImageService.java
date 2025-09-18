package com.vinskao.ty_multiverse_consumer.module.people.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vinskao.ty_multiverse_consumer.module.people.dao.PeopleImageRepository;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.PeopleImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 角色頭像服務類
 *
 * 負責角色頭像相關的業務邏輯處理，包括增刪改查等操作。
 */
@Service
@Transactional(readOnly = true)
public class PeopleImageService {
    private final PeopleImageRepository peopleImageRepository;

    /**
     * 建構函數
     * 
     * @param peopleImageRepository 角色頭像資料庫操作介面
     */
    public PeopleImageService(PeopleImageRepository peopleImageRepository) {
        this.peopleImageRepository = peopleImageRepository;
    }

    /**
     * 獲取所有角色頭像
     *
     * @return 所有角色頭像列表
     */
    public Flux<PeopleImage> getAllPeopleImages() {
        return peopleImageRepository.findAll();
    }

    /**
     * 根據代碼名稱獲取角色頭像
     *
     * @param codeName 角色的代碼名稱
     * @return 角色頭像
     */
    public Mono<PeopleImage> getPeopleImageByCodeName(String codeName) {
        return peopleImageRepository.findByCodeName(codeName);
    }

    /**
     * 檢查角色頭像是否存在
     *
     * @param codeName 角色的代碼名稱
     * @return 如果頭像存在返回 true，否則返回 false
     */
    public Mono<Boolean> peopleImageExists(String codeName) {
        return peopleImageRepository.existsByCodeName(codeName);
    }

    /**
     * 保存角色頭像
     *
     * @param peopleImage 要保存的角色頭像
     * @return 保存後的角色頭像
     */
    @Transactional
    public Mono<PeopleImage> savePeopleImage(PeopleImage peopleImage) {
        return peopleImageRepository.save(peopleImage);
    }

    /**
     * 根據代碼名稱刪除角色頭像
     *
     * @param codeName 角色的代碼名稱
     */
    @Transactional
    public Mono<Void> deletePeopleImage(String codeName) {
        return peopleImageRepository.findByCodeName(codeName)
            .flatMap(peopleImageRepository::delete);
    }
    
    /**
     * 檢查代碼名稱是否唯一
     *
     * @param codeName 要檢查的代碼名稱
     * @return 如果代碼名稱唯一返回 true，否則返回 false
     */
    public Mono<Boolean> isCodeNameUnique(String codeName) {
        return peopleImageRepository.existsByCodeName(codeName)
            .map(exists -> !exists);
    }

    /**
     * 根據代碼名稱列表查詢角色頭像
     *
     * @param codeNames 代碼名稱列表
     * @return 符合條件的角色頭像列表
     */
    public Flux<PeopleImage> findByCodeNames(List<String> codeNames) {
        // ✅ 優化：使用批量查詢，避免N+1問題
        return peopleImageRepository.findByCodeNamesIn(codeNames);
    }
}
