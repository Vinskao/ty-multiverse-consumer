package com.vinskao.ty_multiverse_consumer.module.people.service;

import org.springframework.stereotype.Service;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.people.service.strategy.DamageStrategy;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 武器傷害計算服務 (Consumer 版本)
 * 
 * 負責在 Consumer 端計算角色使用武器時的傷害值。
 */
@Service
public class WeaponDamageService {

    private final WeaponService weaponService;
    private final PeopleService peopleService;
    private final DamageStrategy damageStrategy;

    public WeaponDamageService(WeaponService weaponService, PeopleService peopleService,
            DamageStrategy damageStrategy) {
        this.weaponService = weaponService;
        this.peopleService = peopleService;
        this.damageStrategy = damageStrategy;
    }

    /**
     * 批量計算多個角色的武器傷害值
     *
     * @param names 角色名稱列表
     * @return 包含傷害結果和未找到名稱的 Map
     */
    public Mono<Map<String, Object>> calculateBatchDamageWithWeapon(List<String> names) {
        if (names == null || names.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("damageResults", new HashMap<>());
            emptyResult.put("notFoundNames", new ArrayList<>());
            return Mono.just(emptyResult);
        }

        // 批量查詢所有角色
        return peopleService.findByNames(names).collectList().flatMap(people -> {
            Map<String, People> peopleMap = people.stream()
                    .collect(Collectors.toMap(People::getName, p -> p));

            // 批量查詢所有武器
            return weaponService.getWeaponsByOwners(names).collectList().map(allWeapons -> {
                Map<String, List<Weapon>> weaponsMap = allWeapons.stream()
                        .collect(Collectors.groupingBy(Weapon::getOwner));

                Map<String, Integer> damageResults = new HashMap<>();
                List<String> notFoundNames = new ArrayList<>();

                for (String name : names) {
                    People person = peopleMap.get(name);
                    if (person == null) {
                        notFoundNames.add(name);
                        continue;
                    }

                    List<Weapon> weapons = weaponsMap.getOrDefault(name, new ArrayList<>());
                    int damage = damageStrategy.calculateDamage(person, weapons);
                    damageResults.put(name, damage);
                }

                Map<String, Integer> sortedDamageResults = damageResults.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                java.util.LinkedHashMap::new));

                Map<String, Object> finalResult = new HashMap<>();
                finalResult.put("damageResults", sortedDamageResults);
                finalResult.put("notFoundNames", notFoundNames);
                return finalResult;
            });
        });
    }
}
