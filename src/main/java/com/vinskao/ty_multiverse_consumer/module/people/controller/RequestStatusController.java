package com.vinskao.ty_multiverse_consumer.module.people.controller;

import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.DamageCalculationResult;
import com.vinskao.ty_multiverse_consumer.module.people.service.DamageCalculationResultService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 請求狀態查詢Controller
 * 提供異步請求的狀態查詢功能
 */
@RestController
@RequestMapping("/tymb/api")
@Tag(name = "Request Status", description = "異步請求狀態查詢 API")
public class RequestStatusController {

    @Autowired
    private DamageCalculationResultService damageCalculationResultService;

    @Operation(summary = "檢查傷害計算結果是否存在", description = "檢查指定請求ID的傷害計算結果是否存在")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "檢查成功", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/request-status/{requestId}/exists")
    public ResponseEntity<Map<String, Object>> checkDamageCalculationExists(@Parameter(description = "請求ID") @PathVariable String requestId) {
        boolean exists = damageCalculationResultService.existsByRequestId(requestId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("exists", exists);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "獲取傷害計算結果", description = "獲取指定請求ID的傷害計算結果")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取結果", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "結果不存在"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/request-status/{requestId}")
    public ResponseEntity<Map<String, Object>> getDamageCalculationResult(@Parameter(description = "請求ID") @PathVariable String requestId) {
        var optional = damageCalculationResultService.findByRequestId(requestId);
        
        if (optional.isPresent()) {
            DamageCalculationResult result = optional.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", result.getRequestId());
            response.put("characterName", result.getCharacterName());
            response.put("status", result.getStatus());
            response.put("damageValue", result.getDamageValue());
            response.put("errorMessage", result.getErrorMessage());
            response.put("createdAt", result.getCreatedAt());
            response.put("completedAt", result.getCompletedAt());
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Request not found");
            response.put("requestId", requestId);
            
            return ResponseEntity.notFound().build();
        }
    }
}
