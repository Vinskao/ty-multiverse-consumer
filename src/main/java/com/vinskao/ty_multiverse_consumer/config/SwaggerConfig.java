package com.vinskao.ty_multiverse_consumer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger API 文檔配置
 * 
 * 配置 OpenAPI 3.0 規範的 API 文檔
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TY Multiverse Consumer API")
                        .description("TY Multiverse Consumer 服務的 REST API 文檔")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TY Backend Team")
                                .email("backend@ty.com")
                                .url("https://github.com/ty-backend"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081/ty_multiverse_consumer")
                                .description("本地開發環境"),
                        new Server()
                                .url("https://api.ty.com/ty_multiverse_consumer")
                                .description("生產環境")
                ));
    }
}
