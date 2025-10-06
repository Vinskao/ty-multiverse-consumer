package com.vinskao.ty_multiverse_consumer.module.weapon.exception;

import com.vinskao.ty_multiverse_consumer.core.exception.BusinessException;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;

/**
 * Weapon 模組的特定異常類
 */
public class WeaponException extends BusinessException {
    
    public WeaponException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public WeaponException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public WeaponException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
} 