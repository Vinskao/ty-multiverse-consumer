package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * 异常处理器接口 - 责任链模式
 *
 * 定義責任鏈模式中每個異常處理器的行為
 */
public interface ExceptionHandler {

    /**
     * 處理異常
     *
     * @param ex 要處理的異常
     * @return 如果能處理，返回對應的響應；否則返回 null 讓下一個處理器處理
     */
    Mono<ResponseEntity<ErrorResponse>> handle(Exception ex);

    /**
     * 設置下一個處理器
     *
     * @param nextHandler 下一個處理器
     */
    void setNext(ExceptionHandler nextHandler);

    /**
     * 獲取處理器名稱（用於日誌和調試）
     *
     * @return 處理器名稱
     */
    String getHandlerName();
}
