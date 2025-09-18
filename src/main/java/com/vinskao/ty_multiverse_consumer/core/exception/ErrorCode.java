package com.vinskao.ty_multiverse_consumer.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 統一錯誤代碼枚舉
 */
public enum ErrorCode {

    // 通用錯誤
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "內部伺服器錯誤"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "SYS_002", "無效的請求"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "SYS_003", "找不到資源"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_001", "驗證錯誤"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.BAD_REQUEST, "DATA_001", "數據完整性違規"),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "ARG_001", "無效的參數");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return httpStatus.value();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}