package com.vinskao.ty_multiverse_consumer.core.exception;

import java.time.LocalDateTime;

/**
 * 錯誤響應類
 * 用於統一錯誤響應格式
 */
public class ErrorResponse {

    private final int code;
    private final String message;
    private final String details;
    private final LocalDateTime timestamp;

    /**
     * 創建錯誤響應
     *
     * @param code 錯誤代碼
     * @param message 錯誤訊息
     * @param details 詳細訊息
     */
    public ErrorResponse(int code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
    
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
} 