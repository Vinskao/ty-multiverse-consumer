package com.vinskao.ty_multiverse_consumer.module.weapon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/weapons")
@Tag(name = "Weapon Management", description = "武器管理相關 API")
public class WeaponController {

    @Autowired
    private WeaponService weaponService;

    @Operation(summary = "獲取所有武器", description = "獲取數據庫中所有武器的列表")
    @GetMapping
    public Flux<Weapon> getAllWeapons() {
        return weaponService.getAllWeapons();
    }

    @Operation(summary = "根據名稱獲取武器", description = "根據武器名稱獲取特定武器的信息")
    @GetMapping("/{name}")
    public Mono<ResponseEntity<Weapon>> getWeaponById(@PathVariable String name) {
        return weaponService.getWeaponById(name)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "創建武器", description = "創建一個新武器")
    @PostMapping
    public Mono<ResponseEntity<Weapon>> createWeapon(@RequestBody Weapon weapon) {
        return weaponService.saveWeapon(weapon)
                .map(saved -> ResponseEntity.ok(saved));
    }

    @Operation(summary = "刪除武器", description = "根據名稱刪除武器")
    @DeleteMapping("/{name}")
    public Mono<ResponseEntity<Void>> deleteWeapon(@PathVariable String name) {
        return weaponService.deleteWeapon(name)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
