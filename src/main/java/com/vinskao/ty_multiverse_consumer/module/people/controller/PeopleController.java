package com.vinskao.ty_multiverse_consumer.module.people.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.vinskao.ty_multiverse_consumer.module.people.domain.dto.PeopleNameRequestDTO;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleService;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncMessageService;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/people")
@Tag(name = "People Management", description = "角色管理相關 API")
public class PeopleController {

    private static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    @Autowired
    private PeopleService peopleService;
    
    @Autowired(required = false)
    private AsyncMessageService asyncMessageService;
    


    @Operation(summary = "插入角色", description = "創建一個新的角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "角色創建成功", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/insert")
    public ResponseEntity<?> insertPeople(@RequestBody People people) {
        try {
            People savedPeople = peopleService.insertPerson(people);
            return new ResponseEntity<>(savedPeople, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid input: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "更新角色", description = "更新現有角色的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "角色更新成功", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/update")
    public ResponseEntity<?> updatePeople(@RequestBody People people) {
        try {
            // 驗證輸入
            if (people == null || people.getName() == null || people.getName().trim().isEmpty()) {
                return new ResponseEntity<>("Invalid input: name is required", HttpStatus.BAD_REQUEST);
            }
            
            // 嘗試更新
            People updatedPeople = peopleService.updatePerson(people);
            return new ResponseEntity<>(updatedPeople, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input while updating person", e);
            return new ResponseEntity<>("Person not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (org.hibernate.StaleObjectStateException e) {
            // 樂觀鎖定衝突，返回衝突狀態
            logger.error("Concurrent update detected", e);
            return new ResponseEntity<>("Concurrent update detected: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 樂觀鎖定衝突，返回衝突狀態
            logger.error("Optimistic locking failure detected", e);
            return new ResponseEntity<>("Character data has been modified by another user, please reload and try again", HttpStatus.CONFLICT);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 數據完整性違規，返回錯誤請求狀態
            return new ResponseEntity<>("Data integrity violation: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            logger.error("Runtime exception during update", e);
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Unexpected error during update", e);
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 插入多個 (接收 JSON)
    @PostMapping("/insert-multiple")
    public ResponseEntity<?> insertMultiplePeople(@RequestBody List<People> peopleList) {
        try {
            List<People> savedPeople = peopleService.saveAllPeople(peopleList);
            return new ResponseEntity<>(savedPeople, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid input: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "獲取所有角色", description = "獲取數據庫中所有角色的列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色列表", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "202", description = "異步處理中，請稍後查詢結果"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/get-all")
    public ResponseEntity<?> getAllPeople() {
        // 如果 RabbitMQ 啟用，使用異步處理
        if (asyncMessageService != null) {
            String requestId = asyncMessageService.sendPeopleGetAllRequest();
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("status", "processing");
            response.put("message", "角色列表獲取請求已提交，請稍後查詢結果");
            return ResponseEntity.accepted().body(response);
        }
        
        // 本地環境，同步處理
        try {
            // 使用優化的批量查詢方法，但保持相同的API介面
            List<People> people = peopleService.getAllPeopleOptimized();
            return new ResponseEntity<>(people, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "根據名稱獲取角色", description = "根據角色名稱獲取特定角色的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色信息", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "404", description = "角色不存在"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/get-by-name")
    public ResponseEntity<?> getPeopleByName(@RequestBody PeopleNameRequestDTO request) {
        try {
            Optional<People> people = peopleService.getPeopleByName(request.getName());
            if (people.isPresent()) {
                return new ResponseEntity<>(people.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Person not found", HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "刪除所有角色", description = "刪除數據庫中所有角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "所有角色刪除成功"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/delete-all")
    public ResponseEntity<?> deleteAllPeople() {
        try {
            peopleService.deleteAllPeople();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "獲取所有角色名稱", description = "獲取數據庫中所有角色的名稱列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色名稱列表", 
                    content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/names")
    public ResponseEntity<?> getAllPeopleNames() {
        try {

            
            List<String> names = peopleService.getAllPeopleNames();
            return new ResponseEntity<>(names, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Runtime exception during getAllPeopleNames", e);
            return new ResponseEntity<>("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Unexpected error during getAllPeopleNames", e);
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
