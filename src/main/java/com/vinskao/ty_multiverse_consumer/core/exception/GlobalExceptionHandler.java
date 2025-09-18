package com.vinskao.ty_multiverse_consumer.core.exception;

import com.vinskao.ty_multiverse_consumer.core.exception.handler.ExceptionHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * WebFlux 全局異常處理器 - 責任鏈模式實現
 *
 * 使用責任鏈模式統一處理應用程序中的各種異常
 * 每個異常會按照預定義的優先級順序被處理器鏈處理
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionHandlerChain exceptionHandlerChain;

    public GlobalExceptionHandler(ExceptionHandlerChain exceptionHandlerChain) {
        this.exceptionHandlerChain = exceptionHandlerChain;
        logger.info("✅ GlobalExceptionHandler 初始化完成，使用責任鏈模式處理異常");
    }

    /**
     * 統一異常處理入口
     *
     * 所有異常都會通過責任鏈進行處理
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex) {
        logger.debug("收到異常請求，開始通過責任鏈處理: {}", ex.getClass().getSimpleName());

        return exceptionHandlerChain.handle(ex)
            .doOnSuccess(response -> {
                if (response != null) {
                    logger.debug("異常處理成功: {} -> HTTP {}",
                        ex.getClass().getSimpleName(),
                        response.getStatusCode());
                } else {
                    logger.warn("異常未被任何處理器處理: {}", ex.getClass().getSimpleName());
                }
            })
            .doOnError(chainError -> {
                logger.error("責任鏈處理過程發生錯誤", chainError);
            })
            .onErrorResume(chainError -> {
                // 如果責任鏈本身出錯，返回通用錯誤響應
                ErrorResponse errorResponse = new ErrorResponse(
                    ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                    "系統異常",
                    "異常處理系統發生錯誤"
                );
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            })
            .switchIfEmpty(
                // 如果沒有處理器能處理，返回默認錯誤響應
                Mono.defer(() -> {
                    logger.warn("沒有處理器能處理異常，返回默認響應: {}", ex.getClass().getSimpleName());
                    ErrorResponse errorResponse = new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        "系統異常",
                        "未預期的系統錯誤"
                    );
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                })
            );
    }
}