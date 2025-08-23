package com.vinskao.ty_multiverse_consumer.core.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorCode;
import com.vinskao.ty_multiverse_consumer.core.exception.ErrorResponse;

/**
 * Fallback handler that deals with any exception type not recognized by previous handlers.
 */
@Component
@Order(Integer.MAX_VALUE)
public class DefaultApiExceptionHandler implements ApiExceptionHandler {

    @Override
    public boolean canHandle(Exception ex) {
        return true; // Always handles
    }

    @Override
    public ResponseEntity<ErrorResponse> handle(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.fromErrorCode(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}