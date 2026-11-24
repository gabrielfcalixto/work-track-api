package br.com.gfctech.project_manager.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> handleApiError(ApiException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    ResponseEntity<Map<String, String>> handleUnknownUser(UsuarioNaoEncontradoException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        String message = fields.values().stream().findFirst().orElse("Verifique os dados enviados.");
        return ResponseEntity.badRequest().body(Map.of("message", message, "fields", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> handleMalformedRequest(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Formato inválido em um dos campos enviados."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Há dados duplicados ou registros vinculados a esta operação."));
    }
}
