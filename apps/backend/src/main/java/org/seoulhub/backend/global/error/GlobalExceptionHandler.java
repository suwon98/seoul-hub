package org.seoulhub.backend.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Business Validation Error: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String combinedMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("[%s: %s]", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.warn("DTO Validation Error: {}", combinedMessage);

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(combinedMessage)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .status(409).error("CONFLICT").message("이미 등록된 정보와 충돌합니다. 입력 내용을 확인해 주세요.").build());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("BAD_REQUEST").message("요청 본문의 형식이 올바르지 않습니다.").build());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleCredentials(Exception e) {
        return ResponseEntity.status(401).body(ErrorResponse.builder().status(401).error("UNAUTHORIZED").message("이메일 또는 비밀번호가 올바르지 않습니다.").build());
    }
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ErrorResponse> handleUnverified(Exception e) {
        return ResponseEntity.status(403).body(ErrorResponse.builder().status(403).error("EMAIL_UNVERIFIED").message("이메일 인증을 먼저 완료해 주세요. 인증 메일을 다시 요청할 수 있습니다.").build());
    }
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(Exception e) {
        return ResponseEntity.status(429).header("Retry-After","900").body(ErrorResponse.builder().status(429).error("TOO_MANY_REQUESTS").message("요청이 너무 많습니다. 15분 후 다시 시도해 주세요.").build());
    }
    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameters(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder().status(400).error("BAD_REQUEST").message("요청 항목의 형식을 확인해 주세요.").build());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(ErrorResponse.builder().status(405).error("METHOD_NOT_ALLOWED").message("지원하지 않는 요청 방식입니다.").build());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unhandled Internal Server Error", e);

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .message("서버 내부 에러가 발생했습니다. 관리자에게 문의하세요.")
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
