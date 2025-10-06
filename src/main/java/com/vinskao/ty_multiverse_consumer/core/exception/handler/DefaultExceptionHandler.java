package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 默認異常處理器
 *
 * 處理所有未被其他處理器處理的異常
 * 這個處理器應該放在責任鏈的最後
 */
@Component
public class DefaultExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Exception ex) {
        // 默認處理器處理所有異常
        return true;
    }

    @Override
    protected Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex) {
        logger.error("未預期的異常", ex);

        ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "系統異常",
            "發生未預期的系統錯誤，請聯繫技術支持"
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    @Override
    public String getHandlerName() {
        return "DefaultExceptionHandler";
    }
}
