package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 异常处理器责任链
 *
 * 負責將各個異常處理器組裝成責任鏈，並提供統一的處理入口
 */
@Component
public class ExceptionHandlerChain {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerChain.class);

    private final ExceptionHandler chainHead;

    public ExceptionHandlerChain(
            ValidationExceptionHandler validationHandler,
            BusinessExceptionHandler businessHandler,
            DataIntegrityExceptionHandler dataIntegrityHandler,
            ResilienceExceptionHandler resilienceHandler,
            IllegalArgumentExceptionHandler illegalArgumentHandler,
            RuntimeExceptionHandler runtimeHandler,
            DefaultExceptionHandler defaultHandler) {

        // 按照優先級順序組裝責任鏈
        // 1. 驗證異常（最具體）
        // 2. 業務異常
        // 3. 數據完整性異常
        // 4. 彈性異常
        // 5. 非法參數異常
        // 6. 運行時異常
        // 7. 默認異常（兜底）

        validationHandler.setNext(businessHandler);
        businessHandler.setNext(dataIntegrityHandler);
        dataIntegrityHandler.setNext(resilienceHandler);
        resilienceHandler.setNext(illegalArgumentHandler);
        illegalArgumentHandler.setNext(runtimeHandler);
        runtimeHandler.setNext(defaultHandler);

        this.chainHead = validationHandler;

        logger.info("✅ 異常處理器責任鏈組裝完成");
        logChainStructure();
    }

    /**
     * 處理異常
     *
     * @param ex 要處理的異常
     * @return 處理結果
     */
    public Mono<ResponseEntity<ErrorResponse>> handle(Exception ex) {
        logger.debug("開始處理異常: {}", ex.getClass().getSimpleName());
        return chainHead.handle(ex)
            .doOnSuccess(response -> logger.debug("異常處理完成: {} -> {}",
                ex.getClass().getSimpleName(),
                response != null ? response.getStatusCode() : "未處理"))
            .doOnError(error -> logger.error("異常處理過程發生錯誤", error));
    }

    /**
     * 記錄責任鏈結構（用於調試）
     */
    private void logChainStructure() {
        logger.info("異常處理器責任鏈結構:");
        ExceptionHandler current = chainHead;
        int position = 1;
        while (current != null) {
            logger.info("  {}. {}", position++, current.getHandlerName());
            // 通過反射獲取下一個處理器（如果需要調試）
            try {
                java.lang.reflect.Field nextField = current.getClass().getSuperclass().getDeclaredField("nextHandler");
                nextField.setAccessible(true);
                current = (ExceptionHandler) nextField.get(current);
            } catch (Exception e) {
                break;
            }
        }
    }
}
