// File: acl/common/src/main/java/com/telecom/acl/common/exception/NotificationException.java
package com.telecom.acl.common.exception;

/**
 * 알림 처리 중 발생하는 예외를 처리하는 클래스입니다.
 */
public class NotificationException extends RuntimeException {

    /**
     * 메시지와 함께 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public NotificationException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인 예외와 함께 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}