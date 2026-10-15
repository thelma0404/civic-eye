package com.civic.common.exception;

import com.civic.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns exceptions into friendly JSON messages the frontend can show. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> workerNotFound(WorkerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }

    /**
     * Database rejected the data - usually a value that is too long, a duplicate email,
     * or tables left over from an older version of the project (see README, "Reset the database").
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> dataIntegrity(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        String detail = root == null ? e.getMessage() : root.getMessage();
        log.error("Database rejected the data: {}", detail);
        String lower = detail == null ? "" : detail.toLowerCase();
        String message;
        if (lower.contains("duplicate")) {
            message = "That record already exists (for example, the email is already registered).";
        } else if (lower.contains("too long")) {
            message = "One of the values is too long. Please shorten it and try again.";
        } else if (lower.contains("doesn't have a default value") || lower.contains("cannot be null")) {
            message = "The database tables don't match this version of the app. Reset the database (see README) and restart.";
        } else {
            message = "The database rejected this data: " + detail;
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getReason()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("The file is too large. Please upload an image under 10 MB."));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> missing(Exception e) {
        String name = e instanceof MissingServletRequestParameterException p ? p.getParameterName()
                : ((MissingServletRequestPartException) e).getRequestPartName();
        return ResponseEntity.badRequest().body(ApiResponse.error("Missing required field: " + name));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> unreadable(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Some of the submitted values are invalid."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> method(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        log.error("Unexpected error", e);
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        // Show the underlying cause so problems are easy to diagnose while developing.
        String detail = root.getClass().getSimpleName() + (root.getMessage() != null ? ": " + root.getMessage() : "");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong on our side (" + detail + ")"));
    }
}
