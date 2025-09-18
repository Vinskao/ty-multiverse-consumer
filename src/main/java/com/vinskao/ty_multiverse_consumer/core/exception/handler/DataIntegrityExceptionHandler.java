package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 數據完整性異常處理器
 *
 * 處理數據庫完整性約束違規的異常
 */
@Component
public class DataIntegrityExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        return ex instanceof DataIntegrityViolationException;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        DataIntegrityViolationException dataException = (DataIntegrityViolationException) ex;
        logger.error("數據完整性違規", dataException);

        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.BAD_REQUEST.getCode(),
            "數據完整性違規",
            "請檢查數據是否符合業務規則：" + dataException.getMessage()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "DataIntegrityExceptionHandler";
    }
}
