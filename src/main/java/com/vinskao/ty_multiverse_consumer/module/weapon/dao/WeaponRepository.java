package com.vinskao.ty_multiverse_consumer.module.weapon.dao;

import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@Repository
public interface WeaponRepository extends ReactiveCrudRepository<Weapon, String> {
    
    Mono<Weapon> findByName(String name);
    
    Mono<Boolean> existsByName(String name);
    
    Flux<Weapon> findByOwner(String owner);
    
    // 新增：批量查詢多個擁有者的武器，避免N+1問題
    @Query("SELECT * FROM weapon WHERE owner IN (:owners)")
    Flux<Weapon> findByOwnersIn(@Param("owners") List<String> owners);
    
    @Query("SELECT * FROM weapon WHERE base_damage BETWEEN :minDamage AND :maxDamage")
    Flux<Weapon> findByBaseDamageBetween(@Param("minDamage") Integer minDamage, @Param("maxDamage") Integer maxDamage);
    
    Flux<Weapon> findByAttributes(String attributes);
    
    @Query("SELECT * FROM weapon WHERE :attribute = ANY(bonus_attributes)")
    Flux<Weapon> findByBonusAttributesContaining(@Param("attribute") String attribute);
    
    @Query("SELECT * FROM weapon WHERE :attribute = ANY(state_attributes)")
    Flux<Weapon> findByStateAttributesContaining(@Param("attribute") String attribute);
} 