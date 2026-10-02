package com.vinskao.ty_multiverse_consumer.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ty.common.exception.BusinessException;
import tw.com.ty.common.exception.web.CommonExceptionAutoConfiguration;
import tw.com.ty.common.exception.web.ReactiveExceptionAdvice;
import tw.com.ty.common.response.ErrorCode;

/**
 * The consumer (WebFlux) uses the shared error handling from ty-multiverse-common. There must be exactly
 * one catch-all advice: a second {@code @ExceptionHandler(Exception.class)} would make the winner undefined.
 */
class SharedErrorHandlingTest {

    /** Scans the package that used to hold the consumer's own GlobalExceptionHandler. */
    @Configuration
    @ComponentScan("com.vinskao.ty_multiverse_consumer.core.exception")
    static class ScanLocalExceptionPackage {
    }

    @RestController
    static class Probe {
        @GetMapping("/bad-arg")
        String badArg() {
            throw new IllegalArgumentException("bad");
        }

        @GetMapping("/sandbox")
        String sandbox() {
            throw new SecurityException("sandbox");
        }

        @GetMapping("/unsupported")
        String unsupported() {
            throw new UnsupportedOperationException("nope");
        }

        @GetMapping("/missing")
        String missing() {
            throw new BusinessException(ErrorCode.PEOPLE_NOT_FOUND, "no Bob");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("kaboom");
        }
    }

    private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonExceptionAutoConfiguration.class))
            .withUserConfiguration(ScanLocalExceptionPackage.class);

    @Test
    void context_Should_HaveExactlyOneControllerAdvice_TheSharedReactiveOne() {
        runner.run(ctx -> {
            assertThat(ctx.getBeansWithAnnotation(ControllerAdvice.class).values())
                    .hasSize(1)
                    .allMatch(bean -> bean instanceof ReactiveExceptionAdvice);
        });
    }

    @Test
    void behavior_Should_KeepTheStatusesTheOldConsumerHandlerGave() {
        runner.run(ctx -> {
            WebTestClient client = WebTestClient.bindToController(new Probe())
                    .controllerAdvice(ctx.getBean(ReactiveExceptionAdvice.class)).build();

            client.get().uri("/bad-arg").exchange().expectStatus().isBadRequest();
            client.get().uri("/sandbox").exchange().expectStatus().isForbidden();
            client.get().uri("/unsupported").exchange().expectStatus().isBadRequest();
            client.get().uri("/boom").exchange().expectStatus().is5xxServerError();
            client.get().uri("/missing").exchange().expectStatus().isNotFound()
                    .expectBody().jsonPath("$.path").isEqualTo("/missing")
                    .jsonPath("$.code").isEqualTo(ErrorCode.PEOPLE_NOT_FOUND.getCode());
        });
    }
}
