package com.vinskao.ty_multiverse_consumer.module.people.exception;

import com.vinskao.ty_multiverse_consumer.core.exception.BusinessException;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;

/**
 * People 模組的特定異常類
 */
public class PeopleException extends BusinessException {
    
    public PeopleException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public PeopleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public PeopleException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
} 