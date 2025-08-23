package com.vinskao.ty_multiverse_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info = @Info(
        title = "TY Multiverse Consumer API",
        version = "1.0",
        description = "TY Multiverse Consumer REST API documentation"
    )
)
@SpringBootApplication
public class TyMultiverseConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TyMultiverseConsumerApplication.class, args);
    }
}
