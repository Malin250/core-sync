package com.example.coresyncservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 *
 * <p><b>Security note:</b> Generic (unhandled) exceptions return a fixed
 * "Internal Server Error" message. The real stack trace is logged server-side
 * only — never forwarded to the client — to prevent accidental information
 * leakage.
 */
@ControllerAdvice
@RestController
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Business exceptions ───────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Object> handleInvalidFile(InvalidFileException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimit(RateLimitExceededException ex, WebRequest req) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidToken(InvalidTokenException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Token", ex.getMessage(), req);
    }

    // ── Validation errors ─────────────────────────────────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a       // keep first message if same field appears twice
                ));

        Map<String, Object> body = baseBody(status.value(), "Validation Error", req);
        body.put("errors", fieldErrors);

        return new ResponseEntity<>(body, headers, status);
    }

    // ── Catch-all ─────────────────────────────────────────────

    /**
     * Handles any exception not matched above.
     *
     * <p>The real exception message is logged at ERROR level but is NOT
     * included in the HTTP response to avoid leaking implementation details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest req) {
        log.error("Unhandled exception on request [{}]: {}", req.getDescription(false), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", req);
    }

    // ── Helpers ───────────────────────────────────────────────

    private ResponseEntity<Object> build(HttpStatus status, String error, String message, WebRequest req) {
        Map<String, Object> body = baseBody(status.value(), error, req);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    private Map<String, Object> baseBody(int statusCode, String error, WebRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", statusCode);
        body.put("error", error);
        body.put("path", req.getDescription(false).replace("uri=", ""));
        return body;
    }
}
