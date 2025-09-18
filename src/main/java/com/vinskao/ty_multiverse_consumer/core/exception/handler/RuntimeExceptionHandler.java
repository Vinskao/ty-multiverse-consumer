package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 運行時異常處理器
 *
 * 處理未預期的運行時異常
 */
@Component
public class RuntimeExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof RuntimeException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        RuntimeException runtimeException = (RuntimeException) ex;
        logger.error("運行時異常", runtimeException);

        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "服務器內部錯誤",
            "系統運行時發生未預期錯誤：" + runtimeException.getMessage()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "RuntimeExceptionHandler";
    }
}
