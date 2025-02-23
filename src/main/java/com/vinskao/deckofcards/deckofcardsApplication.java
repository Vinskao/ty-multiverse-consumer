package com.vinskao.deckofcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info = @Info(
        title = "Deck of Cards API",
        version = "1.0",
        description = "Deck of Cards REST API documentation"
    )
)
@SpringBootApplication
public class deckofcardsApplication {
    public static void main(String[] args) {
        SpringApplication.run(deckofcardsApplication.class, args);
    }
} 