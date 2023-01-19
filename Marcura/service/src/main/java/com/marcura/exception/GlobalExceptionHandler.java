package com.marcura.exception;

import com.marcura.model.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.time.Instant;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String FIELD_CONVERSION_EX_MSG = "Cannot convert field %s";
    private static final String BODY_OBJECT_CONSTRAINT_EX_MSG = "Wrong object in the request body";


    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiError> defaultExceptionHandler(Exception ex) {
        log.warn("defaultExceptionHandler: {}", ex.getMessage(), ex);
        final var returnedStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(returnedStatus.value())
                             .body(new ApiError(returnedStatus, returnedStatus.getReasonPhrase(), Instant.now()));
    }

    @ExceptionHandler(value = {
            DuplicateCurrencyExchangeException.class,
            CurrencyExchangeDateSetToFutureException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            CurrencyExchangeRateIsNegativeOrZeroException.class,
            CurrencyExchangeRateOverflowException.class,
            CurrencyExchangeRateScaleOverflowException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiError> handleBadRequestException(Exception ex) {
        log.warn("handleBadRequestException: {}", ex.getMessage(), ex);
        final var returnedStatus = HttpStatus.BAD_REQUEST;
        final var exMsg = resolveExMsg(ex, returnedStatus);
        return ResponseEntity.status(returnedStatus.value())
                             .body(new ApiError(returnedStatus, exMsg, Instant.now()));
    }

    @ExceptionHandler(value = CurrencyExchangeNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(CurrencyExchangeNotFoundException ex) {
        log.warn("handleNotFoundException: {}", ex.getMessage(), ex);
        final var returnedStatus = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(returnedStatus.value())
                             .body(new ApiError(returnedStatus, ex.getMessage(), Instant.now()));
    }

    private String resolveExMsg(Exception ex, HttpStatus returnedStatus) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatchException) {
            return FIELD_CONVERSION_EX_MSG.formatted(mismatchException.getName());
        } else if (ex instanceof HttpMessageNotReadableException ||
                ex instanceof MissingServletRequestParameterException) {
            return returnedStatus.getReasonPhrase();
        } else if (ex instanceof ConstraintViolationException) {
            return BODY_OBJECT_CONSTRAINT_EX_MSG;
        } else {
            return ex.getMessage();
        }
    }
}
