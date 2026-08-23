package com.sergio.financial.error;

import com.sergio.financial.auth.EmailAlreadyExistsException;
import com.sergio.financial.auth.InvalidCredentialsException;
import com.sergio.financial.category.CategoryInUseException;
import com.sergio.financial.category.CategoryNotDeletableException;
import com.sergio.financial.category.CategoryNotFoundException;
import com.sergio.financial.importer.UnsupportedStatementFormatException;
import com.sergio.financial.rule.CategoryRuleNotFoundException;
import com.sergio.financial.transaction.TransactionNotFoundException;
import com.sergio.financial.transaction.InvalidTransactionFilterException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AiUnavailableException.class)
    ResponseEntity<ErrorResponse> handleAiUnavailable(AiUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                "O assistente de IA está indisponível no momento. Tente novamente em instantes.");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> duplicateEmail(EmailAlreadyExistsException exception) {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> invalidCredentials(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException exception) {
        if (isEmailConstraint(exception)) {
            return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }
        return error(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Request conflicts with persisted data.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "VALIDATION_ERROR", "Validation failed.", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> malformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "VALIDATION_ERROR", "Validation failed.", Map.of()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> requestParameter(Exception exception) {
        String parameter = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ((MissingServletRequestParameterException) exception).getParameterName();
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "VALIDATION_ERROR", "Validation failed.",
                Map.of(parameter, "Invalid or missing request parameter.")));
    }

    @ExceptionHandler(UnsupportedStatementFormatException.class)
    ResponseEntity<ErrorResponse> unsupportedStatement(UnsupportedStatementFormatException exception) {
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_STATEMENT_FORMAT", exception.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    ResponseEntity<ErrorResponse> transactionNotFound(TransactionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction was not found.");
    }

    @ExceptionHandler(InvalidTransactionFilterException.class)
    ResponseEntity<ErrorResponse> invalidTransactionFilter(InvalidTransactionFilterException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "VALIDATION_ERROR", exception.getMessage(),
                exception.getFieldErrors()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ResponseEntity<ErrorResponse> categoryNotFound(CategoryNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category was not found.");
    }

    @ExceptionHandler(CategoryRuleNotFoundException.class)
    ResponseEntity<ErrorResponse> categoryRuleNotFound(CategoryRuleNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "CATEGORY_RULE_NOT_FOUND", "Category rule was not found.");
    }

    @ExceptionHandler(CategoryNotDeletableException.class)
    ResponseEntity<ErrorResponse> categoryNotDeletable(CategoryNotDeletableException exception) {
        return error(HttpStatus.CONFLICT, "CATEGORY_NOT_DELETABLE", "System categories cannot be deleted.");
    }

    @ExceptionHandler(CategoryInUseException.class)
    ResponseEntity<ErrorResponse> categoryInUse(CategoryInUseException exception) {
        return error(HttpStatus.CONFLICT, "CATEGORY_IN_USE", "Categories in use cannot be deleted.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, message));
    }

    private boolean isEmailConstraint(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("email")
                    && message.toLowerCase(java.util.Locale.ROOT).contains("app_users")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
