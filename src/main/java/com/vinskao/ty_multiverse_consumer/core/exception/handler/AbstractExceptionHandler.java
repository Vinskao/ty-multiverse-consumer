package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * 抽象异常处理器基类
 *
 * 提供責任鏈模式的基礎實現，子類只需要實現 canHandle() 和 doHandle() 方法
 */
public abstract class AbstractExceptionHandler implements ExceptionHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ExceptionHandler nextHandler;

    @Override
    public void setNext(ExceptionHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public Mono<ResponseEntity<ErrorResponse>> handle(Exception ex) {
        if (canHandle(ex)) {
            logger.debug("處理器 [{}] 正在處理異常: {}", getHandlerName(), ex.getClass().getSimpleName());
            return doHandle(ex);
        }

        // 如果不能處理，傳遞給下一個處理器
        if (nextHandler != null) {
            logger.debug("處理器 [{}] 無法處理，傳遞給下一個處理器", getHandlerName());
            return nextHandler.handle(ex);
        }

        // 如果沒有下一個處理器，返回 null
        logger.warn("處理器 [{}] 無法處理異常，且沒有下一個處理器: {}", getHandlerName(), ex.getClass().getSimpleName());
        return Mono.empty();
    }

    /**
     * 判斷是否能處理此異常
     *
     * @param ex 異常
     * @return true 如果能處理
     */
    protected abstract boolean canHandle(Exception ex);

    /**
     * 執行具體的異常處理邏輯
     *
     * @param ex 異常
     * @return 處理結果
     */
    protected abstract Mono<ResponseEntity<ErrorResponse>> doHandle(Exception ex);
}
