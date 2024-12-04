package com.telecom.acl.service.exception;

import com.telecom.acl.common.exception.ErrorResponse;
import com.telecom.acl.common.exception.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationError(NotificationException e) {
        log.error("알림 처리 오류: {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse("NOTIFICATION_ERROR", e.getMessage());
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse("INTERNAL_SERVER_ERROR", "내부 서버 오류가 발생했습니다");
        return ResponseEntity.status(500).body(response);
    }
}