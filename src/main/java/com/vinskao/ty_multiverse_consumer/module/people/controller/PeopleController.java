package com.vinskao.ty_multiverse_consumer.module.people.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
// import org.springframework.orm.ObjectOptimisticLockingFailureException; // Not needed in R2DBC
import io.swagger.v3.oas.annotations.Operation;
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
    public Mono<ResponseEntity<Object>> insertPeople(@RequestBody People people) {
        return peopleService.insertPerson(people)
            .map(savedPeople -> ResponseEntity.status(HttpStatus.CREATED).body((Object) savedPeople))
            .onErrorResume(IllegalArgumentException.class, e -> Mono.just(ResponseEntity.badRequest().body((Object) ("Invalid input: " + e.getMessage()))))
            .onErrorResume(RuntimeException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage()))))
            .onErrorResume(Exception.class, e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage()))));
    }

    @Operation(summary = "更新角色", description = "更新現有角色的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "角色更新成功", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/update")
    public Mono<ResponseEntity<Object>> updatePeople(@RequestBody People people) {
        // 驗證輸入
        if (people == null || people.getName() == null || people.getName().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("Invalid input: name is required"));
        }

        // 嘗試更新
        return peopleService.updatePerson(people)
            .map(updatedPeople -> ResponseEntity.ok((Object) updatedPeople))
            .onErrorResume(IllegalArgumentException.class, e -> {
                logger.error("Invalid input while updating person", e);
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) ("Person not found: " + e.getMessage())));
            })
            .onErrorResume(org.springframework.dao.DataIntegrityViolationException.class, e ->
                Mono.just(ResponseEntity.badRequest().body((Object) ("Data integrity violation: " + e.getMessage()))))
            .onErrorResume(RuntimeException.class, e -> {
                logger.error("Runtime exception during update", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage())));
            })
            .onErrorResume(Exception.class, e -> {
                logger.error("Unexpected error during update", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage())));
            });
    }

    // 插入多個 (接收 JSON)
    @PostMapping("/insert-multiple")
    public Mono<ResponseEntity<Object>> insertMultiplePeople(@RequestBody List<People> peopleList) {
        return peopleService.saveAllPeople(peopleList)
            .collectList()
            .map(savedPeople -> ResponseEntity.status(HttpStatus.CREATED).body((Object) savedPeople))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.badRequest().body((Object) ("Invalid input: " + e.getMessage()))))
            .onErrorResume(RuntimeException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage()))))
            .onErrorResume(Exception.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage()))));
    }

    @Operation(summary = "獲取所有角色", description = "獲取數據庫中所有角色的列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色列表", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "202", description = "異步處理中，請稍後查詢結果"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/get-all")
    public Mono<ResponseEntity<Object>> getAllPeople() {
        // 如果 RabbitMQ 啟用，使用異步處理
        if (asyncMessageService != null) {
            String requestId = asyncMessageService.sendPeopleGetAllRequest();
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("status", "processing");
            response.put("message", "角色列表獲取請求已提交，請稍後查詢結果");
            return Mono.just(ResponseEntity.accepted().body(response));
        }

        // 本地環境，同步處理
        return peopleService.getAllPeopleOptimized()
            .collectList()
            .map(people -> ResponseEntity.ok((Object) people))
            .onErrorResume(RuntimeException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage()))))
            .onErrorResume(Exception.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage()))));
    }

    @Operation(summary = "根據名稱獲取角色", description = "根據角色名稱獲取特定角色的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色信息", 
                    content = @Content(schema = @Schema(implementation = People.class))),
        @ApiResponse(responseCode = "404", description = "角色不存在"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/get-by-name")
    public Mono<ResponseEntity<Object>> getPeopleByName(@RequestBody PeopleNameRequestDTO request) {
        return peopleService.getPeopleByName(request.getName())
            .map(people -> ResponseEntity.ok((Object) people))
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) "Person not found"))
            .onErrorResume(RuntimeException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage()))))
            .onErrorResume(Exception.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage()))));
    }

    @Operation(summary = "刪除所有角色", description = "刪除數據庫中所有角色")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "所有角色刪除成功"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @PostMapping("/delete-all")
    public Mono<ResponseEntity<Object>> deleteAllPeople() {
        return peopleService.deleteAllPeople()
            .then(Mono.just(ResponseEntity.noContent().build()))
            .onErrorResume(RuntimeException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage()))))
            .onErrorResume(Exception.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage()))));
    }

    @Operation(summary = "獲取所有角色名稱", description = "獲取數據庫中所有角色的名稱列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取角色名稱列表", 
                    content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    @GetMapping("/names")
    public Mono<ResponseEntity<Object>> getAllPeopleNames() {
        return peopleService.getAllPeopleNames()
            .collectList()
            .map(names -> ResponseEntity.ok((Object) names))
            .onErrorResume(RuntimeException.class, e -> {
                logger.error("Runtime exception during getAllPeopleNames", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Internal server error: " + e.getMessage())));
            })
            .onErrorResume(Exception.class, e -> {
                logger.error("Unexpected error during getAllPeopleNames", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) ("Unexpected error: " + e.getMessage())));
            });
    }
}
