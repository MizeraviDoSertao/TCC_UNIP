package com.unip.fraud.adapter.in;

import com.unip.fraud.application.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> invalidRequest(final IllegalArgumentException exception) {
    return response(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> notFound(final ResourceNotFoundException exception) {
    return response(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> fileTooLarge(
      final MaxUploadSizeExceededException exception) {
    return response(HttpStatus.CONTENT_TOO_LARGE, "The dataset exceeds the upload size limit");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> unreadableRequest(
      final HttpMessageNotReadableException exception) {
    return response(HttpStatus.BAD_REQUEST, "Request body is invalid");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validationFailed(
      final MethodArgumentNotValidException exception) {
    final String message = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining("; "));
    return response(HttpStatus.BAD_REQUEST, message);
  }

  private ResponseEntity<ApiError> response(
      final HttpStatus status,
      final String message) {
    return ResponseEntity.status(status).body(
        new ApiError(status.value(), status.getReasonPhrase(), message, LocalDateTime.now())
    );
  }

  public record ApiError(
      int status,
      String error,
      String message,
      LocalDateTime timestamp
  ) {
  }
}
