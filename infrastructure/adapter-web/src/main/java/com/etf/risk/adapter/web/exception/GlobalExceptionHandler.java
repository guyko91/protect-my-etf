package com.etf.risk.adapter.web.exception;

import com.etf.risk.adapter.web.common.ErrorResponse;
import com.etf.risk.domain.exception.DomainException;
import com.etf.risk.domain.exception.DuplicatePositionException;
import com.etf.risk.domain.exception.InsufficientQuantityException;
import com.etf.risk.domain.exception.InvalidQuantityException;
import com.etf.risk.domain.exception.PositionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.of("입력값 검증에 실패했습니다", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.of("입력값 검증에 실패했습니다", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getMessage(), "INVALID_ARGUMENT");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getMessage(), "INVALID_STATE");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
        DuplicatePositionException.class,
        PositionNotFoundException.class,
        InvalidQuantityException.class,
        InsufficientQuantityException.class
    })
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        log.warn("DomainException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getMessage(), e.getClass().getSimpleName());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception", e);
        ErrorResponse response = ErrorResponse.of("서버 내부 오류가 발생했습니다", "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
