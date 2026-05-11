package ru.tischenko.vk.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.tischenko.vk.service.Exceptions.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));
        return ResponseEntity.badRequest().body(body(400, "Bad Request", "Validation failed", req, fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(body(400, "Bad Request", ex.getMessage(), req, null));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> badRequest(Exception ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(body(400, "Bad Request", "Malformed request", req, null));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> notFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(404, "Not Found", ex.getMessage(), req, null));
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<?> conflict(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(409, "Conflict", ex.getMessage(), req, null));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<?> business(BusinessRuleException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(409, "Conflict", ex.getMessage(), req, null));
    }

    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    ResponseEntity<?> auth(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(401, "Unauthorized", "Invalid credentials", req, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(403, "Forbidden", "Access denied", req, null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> generic(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req == null ? "?" : req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body(500, "Internal Server Error", "Unexpected error", req, null));
    }

    // Convenience overloads used by unit tests that do not provide HttpServletRequest.
    ResponseEntity<?> auth(Exception ex) { return auth(ex, null); }
    ResponseEntity<?> denied(AccessDeniedException ex) { return denied(ex, null); }
    ResponseEntity<?> business(BusinessRuleException ex) { return business(ex, null); }
    ResponseEntity<?> notFound(NotFoundException ex) { return notFound(ex, null); }

    private Map<String, Object> body(int status, String error, String message, HttpServletRequest req, Map<String, String> fieldErrors) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status);
        map.put("error", error);
        map.put("message", message);
        map.put("path", req == null ? "" : req.getRequestURI());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            map.put("fieldErrors", fieldErrors);
        }
        return map;
    }
}
