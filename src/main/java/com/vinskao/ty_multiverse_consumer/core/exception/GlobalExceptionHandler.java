package com.vinskao.ty_multiverse_consumer.core.exception;

import tw.com.ty.common.exception.BusinessException;
import tw.com.ty.common.exception.UnifiedErrorConverter;
import tw.com.ty.common.response.ErrorCode;
import tw.com.ty.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * WebFlux 全局異常處理器 - 統一錯誤處理實現
 *
 * 使用統一錯誤轉換器處理所有異常，支援響應式環境
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler() {
        logger.info("✅ GlobalExceptionHandler 初始化完成，使用統一錯誤轉換器處理異常");
    }

    /**
     * 統一異常處理入口
     *
     * 所有異常都會通過統一錯誤轉換器進行處理
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex) {
        logger.debug("收到異常請求，開始統一處理: {}", ex.getClass().getSimpleName());

        try {
            // 將一般異常轉換為業務異常
            BusinessException businessException = UnifiedErrorConverter.convertToBusinessException(ex);

            // 使用統一錯誤轉換器
            ErrorResponse errorResponse = UnifiedErrorConverter.toHttpResponse(
                businessException, "/consumer");

            return Mono.just(new ResponseEntity<>(errorResponse, businessException.getErrorCode().getHttpStatus()));

        } catch (Exception conversionError) {
            logger.error("異常轉換失敗，使用默認錯誤響應", conversionError);

            // 最終的默認錯誤響應
            ErrorResponse errorResponse = ErrorResponse.fromErrorCode(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "系統異常：" + ex.getMessage(),
                "/consumer"
            );

            return Mono.just(new ResponseEntity<>(errorResponse, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()));
        }
    }
}