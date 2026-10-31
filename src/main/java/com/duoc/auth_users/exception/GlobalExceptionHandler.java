package com.duoc.auth_users.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.duoc.auth_users.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> handleConflicto(ConflictoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleArgumentoInvalido(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ---------- Errores del request ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Error de validacion", detalles);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoInvalido(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "El cuerpo de la peticion no es valido (JSON mal formado o valor no permitido)");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "El parametro '" + ex.getName() + "' tiene un formato invalido");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaNoEncontrada(NoResourceFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "La ruta solicitada no existe");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "El metodo " + ex.getMethod() + " no esta permitido para esta ruta");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // ---------- Otros errores ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErrorInesperado(Exception ex) {
        log.error("Error inesperado", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrio un error inesperado, intente mas tarde");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
