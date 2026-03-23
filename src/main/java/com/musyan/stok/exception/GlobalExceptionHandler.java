package com.musyan.stok.exception;

import com.musyan.stok.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleProductAlreadyExistsException(
            ProductAlreadyExistsException exception,
            HttpServletRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientStockException(
            InsufficientStockException exception,
            HttpServletRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception exception,
            HttpServletRequest request) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}