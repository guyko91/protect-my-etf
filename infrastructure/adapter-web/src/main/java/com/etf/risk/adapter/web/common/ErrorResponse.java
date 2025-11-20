package com.etf.risk.adapter.web.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final String message;
    private final String errorCode;
    private final List<FieldError> fieldErrors;
    private final LocalDateTime timestamp;

    private ErrorResponse(String message, String errorCode, List<FieldError> fieldErrors) {
        this.message = message;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null, null);
    }

    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(message, errorCode, null);
    }

    public static ErrorResponse of(String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(message, "VALIDATION_ERROR", fieldErrors);
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
