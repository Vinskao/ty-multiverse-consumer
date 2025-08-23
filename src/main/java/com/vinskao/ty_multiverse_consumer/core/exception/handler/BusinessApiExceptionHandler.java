package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.core.exception.BusinessException;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;

/**
 * Handles {@link BusinessException} instances.
 */
@Component
@Order(0)
public class BusinessApiExceptionHandler implements ApiExceptionHandler {

    @Override
    public boolean canHandle(Exception ex) {
        return ex instanceof BusinessException;
    }

    @Override
    public ResponseEntity<ErrorResponse> handle(Exception ex, HttpServletRequest request) {
        BusinessException be = (BusinessException) ex;
        ErrorResponse response = ErrorResponse.fromBusinessException(be, request.getRequestURI());
        return new ResponseEntity<>(response, be.getErrorCode().getHttpStatus());
    }
}