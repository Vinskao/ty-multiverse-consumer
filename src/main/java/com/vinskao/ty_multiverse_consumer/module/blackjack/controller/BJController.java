package com.vinskao.ty_multiverse_consumer.module.blackjack.controller;

import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/blackjack")
@Tag(name = "Blackjack Controller", description = "Blackjack game operations API")
public class BJController {

    @Autowired
    private BJService bjService;

    @Operation(summary = "Start a new game", description = "Initializes a new blackjack game")
    @PostMapping("/start")
    public ResponseEntity<?> startGame() {
        try {
            return ResponseEntity.ok(bjService.startNewGame());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Failed to start new game: " + e.getMessage());
        }
    }

    @Operation(summary = "Player hits", description = "Player draws a new card")
    @PostMapping("/hit")
    public ResponseEntity<?> hit() {    
        try {
            return ResponseEntity.ok(bjService.playerHit());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body("Invalid hit action: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error during hit action: " + e.getMessage());
        }
    }

    @Operation(summary = "Player stands", description = "Player ends their turn")
    @PostMapping("/stand")
    public ResponseEntity<?> stand() {
        try {
            return ResponseEntity.ok(bjService.playerStand());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body("Invalid stand action: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error during stand action: " + e.getMessage());
        }
    }

    @Operation(summary = "Get game status", description = "Retrieves the current state of the game")
    @GetMapping("/status")
    public ResponseEntity<?> getGameStatus() {
        try {
            return ResponseEntity.ok(bjService.getGameStatus());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body("Cannot get game status: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error getting game status: " + e.getMessage());
        }
    }

    @Operation(summary = "Double down", description = "Player doubles their bet and receives one more card")
    @PostMapping("/double")
    public ResponseEntity<?> doubleDown() {
        try {
            return ResponseEntity.ok(bjService.playerDouble());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body("Invalid double down action: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error during double down: " + e.getMessage());
        }
    }

    @Operation(summary = "Split cards", description = "Player splits their pair into two separate hands")
    @PostMapping("/split")
    public ResponseEntity<?> split() {
        try {
            return ResponseEntity.ok(bjService.playerSplit());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body("Invalid split action: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error during split: " + e.getMessage());
        }
    }
}
