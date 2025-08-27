package com.vinskao.ty_multiverse_consumer.module.weapon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.vinskao.ty_multiverse_consumer.module.weapon.service.WeaponService;
import com.vinskao.ty_multiverse_consumer.module.weapon.domain.vo.Weapon;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weapons")
@Tag(name = "Weapon Management", description = "武器管理相關 API")
public class WeaponController {

    @Autowired
    private WeaponService weaponService;

    @Operation(summary = "獲取所有武器", description = "獲取數據庫中所有武器的列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取武器列表", 
                    content = @Content(schema = @Schema(implementation = Weapon.class))),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping
    public ResponseEntity<List<Weapon>> getAllWeapons() {
        return ResponseEntity.ok(weaponService.getAllWeapons());
    }

    @Operation(summary = "根據名稱獲取武器", description = "根據武器名稱獲取特定武器的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取武器信息", 
                    content = @Content(schema = @Schema(implementation = Weapon.class))),
        @ApiResponse(responseCode = "404", description = "武器不存在"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/{name}")
    public ResponseEntity<Weapon> getWeaponById(@Parameter(description = "武器名稱") @PathVariable String name) {
        return weaponService.getWeaponById(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "根據擁有者獲取武器", description = "獲取特定擁有者的所有武器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取武器列表", 
                    content = @Content(schema = @Schema(implementation = Weapon.class))),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/owner/{owner}")
    public ResponseEntity<List<Weapon>> getWeaponsByOwner(@Parameter(description = "武器擁有者") @PathVariable String owner) {
        return ResponseEntity.ok(weaponService.getWeaponsByOwner(owner));
    }

    @Operation(summary = "創建或更新武器", description = "創建新武器或更新現有武器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "武器保存成功", 
                    content = @Content(schema = @Schema(implementation = Weapon.class))),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping
    public ResponseEntity<Weapon> saveWeapon(@RequestBody Weapon weapon) {
        return ResponseEntity.ok(weaponService.saveWeaponSmart(weapon));
    }

    @Operation(summary = "刪除武器", description = "根據武器名稱刪除特定武器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "武器刪除成功"),
        @ApiResponse(responseCode = "404", description = "武器不存在"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteWeapon(@Parameter(description = "武器名稱") @PathVariable String name) {
        weaponService.deleteWeapon(name);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "刪除所有武器", description = "刪除數據庫中所有武器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "所有武器刪除成功"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> deleteAllWeapons() {
        weaponService.deleteAllWeapons();
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if weapon exists by name (ID)
     */
    @GetMapping("/exists/{name}")
    public ResponseEntity<Map<String, Boolean>> checkWeaponExists(@PathVariable String name) {
        return ResponseEntity.ok(Map.of("exists", weaponService.weaponExists(name)));
    }

    /**
     * Update weapon attributes
     */
    @PutMapping("/{name}/attributes")
    public ResponseEntity<Weapon> updateWeaponAttributes(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {
        Weapon w = weaponService.getWeaponById(name)
                .orElseThrow(() -> new RuntimeException("Weapon not found: " + name));
        w.setAttributes(request.get("attributes"));
        return ResponseEntity.ok(weaponService.updateWeaponAttributes(name, w));
    }

    /**
     * Update weapon base damage
     */
    @PutMapping("/{name}/base-damage")
    public ResponseEntity<Weapon> updateWeaponBaseDamage(
            @PathVariable String name,
            @RequestBody Map<String, Integer> request) {
        return ResponseEntity.ok(weaponService.updateWeaponBaseDamage(name, request.get("baseDamage")));
    }

    /**
     * Update weapon bonus damage
     */
    @PutMapping("/{name}/bonus-damage")
    public ResponseEntity<Weapon> updateWeaponBonusDamage(
            @PathVariable String name,
            @RequestBody Map<String, Integer> request) {
        return ResponseEntity.ok(weaponService.updateWeaponBonusDamage(name, request.get("bonusDamage")));
    }

    /**
     * Update weapon bonus attributes
     */
    @PutMapping("/{name}/bonus-attributes")
    public ResponseEntity<Weapon> updateWeaponBonusAttributes(
            @PathVariable String name,
            @RequestBody Map<String, List<String>> request) {
        return ResponseEntity.ok(weaponService.updateWeaponBonusAttributes(name, request.get("bonusAttributes")));
    }

    /**
     * Update weapon state attributes
     */
    @PutMapping("/{name}/state-attributes")
    public ResponseEntity<Weapon> updateWeaponStateAttributes(
            @PathVariable String name,
            @RequestBody Map<String, List<String>> request) {
        return ResponseEntity.ok(weaponService.updateWeaponStateAttributes(name, request.get("stateAttributes")));
    }

    /**
     * Find weapons by base damage range
     */
    @GetMapping("/damage-range")
    public ResponseEntity<List<Weapon>> findByBaseDamageRange(
            @RequestParam Integer minDamage,
            @RequestParam Integer maxDamage) {
        return ResponseEntity.ok(weaponService.findByBaseDamageRange(minDamage, maxDamage));
    }

    /**
     * Find weapons by attribute
     */
    @GetMapping("/attribute/{attribute}")
    public ResponseEntity<List<Weapon>> findByAttribute(@PathVariable String attribute) {
        return ResponseEntity.ok(weaponService.findByAttribute(attribute));
    }
} 