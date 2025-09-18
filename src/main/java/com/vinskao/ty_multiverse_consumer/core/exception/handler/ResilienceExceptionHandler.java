package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import com.vinskao.ty_multiverse_consumer.core.exception.ResilienceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 彈性異常處理器
 *
 * 處理 Rate Limiter 和 Circuit Breaker 等彈性機制相關的異常
 */
@Component
public class ResilienceExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof ResilienceException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        ResilienceException resilienceException = (ResilienceException) ex;
        logger.warn("彈性機制異常: {}", resilienceException.getErrorType());

        ErrorResponse errorResponse = new ErrorResponse(
            resilienceException.getErrorCode().getCode(),
            resilienceException.getMessage(),
            String.format("錯誤類型: %s, 建議重試時間: %s",
                resilienceException.getErrorType(),
                resilienceException.getRetryAfter())
        );

        // 對於 Rate Limit，返回 429 Too Many Requests
        HttpStatus status = "RATE_LIMIT_EXCEEDED".equals(resilienceException.getErrorType())
            ? HttpStatus.TOO_MANY_REQUESTS
            : HttpStatus.INTERNAL_SERVER_ERROR;

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "ResilienceExceptionHandler";
    }
}
