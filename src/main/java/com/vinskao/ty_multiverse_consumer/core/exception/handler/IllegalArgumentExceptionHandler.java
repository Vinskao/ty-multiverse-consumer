package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 非法參數異常處理器
 *
 * 處理參數不合法的異常
 */
@Component
public class IllegalArgumentExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof IllegalArgumentException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        IllegalArgumentException illegalArgException = (IllegalArgumentException) ex;
        logger.error("非法參數異常", illegalArgException);

        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.BAD_REQUEST.getCode(),
            illegalArgException.getMessage(),
            "請檢查輸入參數的有效性"
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "IllegalArgumentExceptionHandler";
    }
}
