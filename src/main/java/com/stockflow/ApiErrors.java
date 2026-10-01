package com.stockflow;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(InventoryService.Conflict.class)
  public ResponseEntity<?> conflict(RuntimeException e) {
    return error(409, e.getMessage());
  }

  @ExceptionHandler(InventoryService.Missing.class)
  public ResponseEntity<?> missing(RuntimeException e) {
    return error(404, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
    return error(
        400,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> malformed(Exception e) {
    return error(400, "Malformed JSON or invalid field type");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> duplicate(Exception e) {
    return error(409, "Conflicting data; SKU must be unique");
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<?> busy(Exception e) {
    return error(409, "Inventory is busy; retry the operation");
  }

  private ResponseEntity<?> error(int status, String message) {
    return ResponseEntity.status(status).body(Map.of("message", message, "status", status));
  }
}
