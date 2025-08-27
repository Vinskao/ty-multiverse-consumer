package com.vinskao.ty_multiverse_consumer.module.people.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.vinskao.ty_multiverse_consumer.module.people.service.WeaponDamageService;
import com.vinskao.ty_multiverse_consumer.module.people.domain.dto.BatchDamageRequestDTO;
import com.vinskao.ty_multiverse_consumer.module.people.domain.dto.BatchDamageResponseDTO;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncMessageService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/people")
@Tag(name = "Weapon Damage Calculation", description = "武器傷害計算相關 API")
public class WeaponDamageController {

    private final WeaponDamageService weaponDamageService;
    
    @Autowired(required = false)
    private AsyncMessageService asyncMessageService;
    


    public WeaponDamageController(WeaponDamageService weaponDamageService) {
        this.weaponDamageService = weaponDamageService;
    }

    @Operation(summary = "計算武器傷害", description = "計算指定角色的武器傷害值")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "傷害計算成功", 
                    content = @Content(schema = @Schema(implementation = Integer.class))),
        @ApiResponse(responseCode = "202", description = "異步處理中，請稍後查詢結果"),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/damageWithWeapon")
    public ResponseEntity<?> damageWithWeapon(@Parameter(description = "角色名稱") @RequestParam("name") String name) {
        // 如果 RabbitMQ 啟用，使用異步處理
        if (asyncMessageService != null) {
            String requestId = asyncMessageService.sendDamageCalculationRequest(name);
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("status", "processing");
            response.put("message", "傷害計算請求已提交，請稍後查詢結果");
            return ResponseEntity.accepted().body(response);
        }
        
        // 本地環境，同步處理
        int result = weaponDamageService.calculateDamageWithWeapon(name);
        if (result == -1) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "批量計算武器傷害", description = "批量計算多個角色的武器傷害值")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量傷害計算成功", 
                    content = @Content(schema = @Schema(implementation = BatchDamageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/batchDamageWithWeapon")
    public ResponseEntity<BatchDamageResponseDTO> batchDamageWithWeapon(@RequestBody BatchDamageRequestDTO request) {
        BatchDamageResponseDTO result = weaponDamageService.calculateBatchDamageWithWeapon(request);
        return ResponseEntity.ok(result);
    }

    // Removed safeInt method as the computation is now inside the service
} 