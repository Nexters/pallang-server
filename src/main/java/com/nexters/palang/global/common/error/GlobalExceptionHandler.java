package com.nexters.palang.global.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 로직 예외 처리
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        BaseErrorCode errorCode = e.getErrorCode();
        log.warn("AppException 발생: {}, 에러가 발생한 지점: {} {}", errorCode.getMessage(), request.getMethod(), request.getRequestURI());
        
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(request.getRequestURI(), errorCode));
    }

    // @Valid 검사 실패(필수 파라미터가 null 또는 공백) 인 경우 예외를 잡는 핸들러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.error("MethodArgumentNotValidException 발생: {}", detail);
        log.error("에러가 발생한 지점: {} {}", request.getMethod(), request.getRequestURI());
        
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(request.getRequestURI(), GlobalErrorCode.INVALID_INPUT_VALUE, detail));
    }

    // 지원하지 않는 HTTP 메서드 호출 시 예외를 잡는 핸들러
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.error("HttpRequestMethodNotSupportedException 발생: {}", e.getMessage());
        log.error("에러가 발생한 지점: {} {}", request.getMethod(), request.getRequestURI());
        
        return ResponseEntity.status(GlobalErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ErrorResponse.of(request.getRequestURI(), GlobalErrorCode.METHOD_NOT_ALLOWED));
    }

    // 그 외 서버에서 처리되지 않은 모든 예외를 잡는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);
        log.error("에러가 발생한 지점: {} {}", request.getMethod(), request.getRequestURI());
        
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(request.getRequestURI(), GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}
