package com.vinskao.ty_multiverse_consumer.module.weapon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vinskao.ty_multiverse_consumer.module.weapon.dao.WeaponRepository;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class WeaponService {

    private final WeaponRepository weaponRepository;

    public WeaponService(WeaponRepository weaponRepository) {
        this.weaponRepository = weaponRepository;
    }

    /**
     * Get all weapons
     *
     * @return flux of all weapons
     */
    public Flux<Weapon> getAllWeapons() {
        return weaponRepository.findAll();
    }

    /**
     * Get weapons by owner
     *
     * @param owner the weapon owner
     * @return flux of weapons belonging to the specified owner
     */
    public Flux<Weapon> getWeaponsByOwner(String owner) {
        return weaponRepository.findByOwner(owner);
    }

    /**
     * Get weapons by multiple owners (batch query)
     *
     * @param owners list of weapon owners
     * @return flux of weapons belonging to the specified owners
     */
    public Flux<Weapon> getWeaponsByOwners(List<String> owners) {
        return weaponRepository.findByOwnersIn(owners);
    }

    /**
     * Get weapon by name (ID)
     */
    public Mono<Weapon> getWeaponById(String name) {
        return weaponRepository.findById(name);
    }

    /**
     * Save or update a weapon
     * 為了避免版本衝突，先刪除再插入（適用於批量同步場景）
     */
    @Transactional
    public Mono<Weapon> saveWeapon(Weapon weapon) {
        // 先刪除可能存在的舊記錄，再插入新記錄
        // 這樣可以避免樂觀鎖版本衝突
        return weaponRepository.deleteById(weapon.getName())
                .then(weaponRepository.save(weapon))
                .onErrorResume(error -> {
                    // 如果刪除失敗（記錄不存在），直接插入
                    return weaponRepository.save(weapon);
                });
    }

    /**
     * Save or update a weapon with smart field update
     * Only updates non-null and non-empty string fields
     * Embedding field is not handled - managed by external AI services
     */
    @Transactional
    public Mono<Weapon> saveWeaponSmart(Weapon weapon) {
        // 如果是新武器，確保不處理 embedding 欄位
        weapon.setEmbedding(null);

        // 檢查是否為更新操作（武器已存在）
        if (weapon.getName() != null) {
            return weaponRepository.findById(weapon.getName())
                    .flatMap(existing -> updateWeaponSmart(existing, weapon))
                    .switchIfEmpty(weaponRepository.save(weapon));
        }

        return weaponRepository.save(weapon);
    }

    /**
     * Smart update weapon - only update non-null and non-empty fields
     */
    @Transactional
    public Mono<Weapon> updateWeaponSmart(Weapon existing, Weapon updateData) {
        // 只更新非空且非空字串的欄位
        if (isValidString(updateData.getOwner())) {
            existing.setOwner(updateData.getOwner());
        }

        if (isValidString(updateData.getAttributes())) {
            existing.setAttributes(updateData.getAttributes());
        }

        if (updateData.getBaseDamage() != null) {
            existing.setBaseDamage(updateData.getBaseDamage());
        }

        if (updateData.getBonusDamage() != null) {
            existing.setBonusDamage(updateData.getBonusDamage());
        }

        if (updateData.getBonusAttributes() != null && !updateData.getBonusAttributes().isEmpty()) {
            existing.setBonusAttributes(updateData.getBonusAttributes());
        }

        if (updateData.getStateAttributes() != null && !updateData.getStateAttributes().isEmpty()) {
            existing.setStateAttributes(updateData.getStateAttributes());
        }

        // 不處理 embedding 欄位 - 由外部 AI 服務管理

        // 更新時間戳
        existing.setUpdatedAt(LocalDateTime.now());

        return weaponRepository.save(existing);
    }

    /**
     * Check if a string is valid (not null and not empty)
     */
    private boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Delete a weapon by name (ID)
     */
    @Transactional
    public Mono<Void> deleteWeapon(String name) {
        return weaponRepository.deleteById(name);
    }

    /**
     * Delete all weapons
     */
    @Transactional
    public Mono<Void> deleteAllWeapons() {
        return weaponRepository.deleteAll();
    }

    /**
     * Check if a weapon exists by name (ID)
     */
    public Mono<Boolean> weaponExists(String name) {
        return weaponRepository.existsById(name);
    }

    /**
     * Update weapon attributes
     */
    @Transactional
    public Mono<Weapon> updateWeaponAttributes(String name, Weapon newWeapon) {
        return weaponRepository.findById(name)
                .flatMap(existing -> {
                    if (newWeapon.getBaseDamage() != null) {
                        existing.setBaseDamage(newWeapon.getBaseDamage());
                    }
                    if (newWeapon.getAttributes() != null) {
                        existing.setAttributes(newWeapon.getAttributes());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    return weaponRepository.save(existing);
                });
    }

    /**
     * Update weapon base damage
     */
    @Transactional
    public Mono<Weapon> updateWeaponBaseDamage(String name, Integer baseDamage) {
        return weaponRepository.findById(name)
                .flatMap(existing -> {
                    existing.setBaseDamage(baseDamage);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return weaponRepository.save(existing);
                });
    }

    /**
     * Update weapon bonus damage
     */
    @Transactional
    public Mono<Weapon> updateWeaponBonusDamage(String name, Integer bonusDamage) {
        return weaponRepository.findById(name)
                .flatMap(existing -> {
                    existing.setBonusDamage(bonusDamage);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return weaponRepository.save(existing);
                });
    }

    /**
     * Update weapon bonus attributes
     */
    @Transactional
    public Mono<Weapon> updateWeaponBonusAttributes(String name, List<String> bonusAttributes) {
        return weaponRepository.findById(name)
                .flatMap(existing -> {
                    existing.setBonusAttributes(bonusAttributes);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return weaponRepository.save(existing);
                });
    }

    /**
     * Update weapon state attributes
     */
    @Transactional
    public Mono<Weapon> updateWeaponStateAttributes(String name, List<String> stateAttributes) {
        return weaponRepository.findById(name)
                .flatMap(existing -> {
                    existing.setStateAttributes(stateAttributes);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return weaponRepository.save(existing);
                });
    }

    /**
     * Find weapons by base damage range
     */
    public Flux<Weapon> findByBaseDamageRange(Integer minDamage, Integer maxDamage) {
        return weaponRepository.findByBaseDamageBetween(minDamage, maxDamage);
    }

    /**
     * Find weapons by attribute
     */
    public Flux<Weapon> findByAttribute(String attribute) {
        return weaponRepository.findByAttributes(attribute);
    }

    /**
     * Find weapons by bonus attributes containing specific attribute
     */
    public Flux<Weapon> findByBonusAttribute(String attribute) {
        return weaponRepository.findByBonusAttributesContaining(attribute);
    }

    /**
     * Find weapons by state attributes containing specific attribute
     */
    public Flux<Weapon> findByStateAttribute(String attribute) {
        return weaponRepository.findByStateAttributesContaining(attribute);
    }
}