package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

/**
 * 驗證異常處理器
 *
 * 處理參數驗證失敗的異常
 */
@Component
public class ValidationExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof WebExchangeBindException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        WebExchangeBindException bindException = (WebExchangeBindException) ex;
        logger.error("參數驗證異常", bindException);

        String errorMessage = bindException.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("參數驗證失敗");

        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.BAD_REQUEST.getCode(),
            errorMessage,
            "請檢查輸入參數格式"
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "ValidationExceptionHandler";
    }
}
