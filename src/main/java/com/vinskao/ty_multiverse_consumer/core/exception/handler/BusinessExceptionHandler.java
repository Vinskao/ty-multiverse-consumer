package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.BusinessException;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 業務異常處理器
 *
 * 處理業務邏輯相關的異常
 */
@Component
public class BusinessExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof BusinessException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        BusinessException businessException = (BusinessException) ex;
        logger.error("業務異常", businessException);

        ErrorResponse errorResponse = new ErrorResponse(
            businessException.getErrorCode().getCode(),
            businessException.getMessage(),
            businessException.getMessage()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "BusinessExceptionHandler";
    }
}
