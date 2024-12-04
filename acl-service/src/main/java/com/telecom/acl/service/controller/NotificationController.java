// File: acl/acl-service/src/main/java/com/telecom/acl/service/controller/NotificationController.java
package com.telecom.acl.service.controller;

import com.telecom.acl.common.soap.ExcessNotificationSoap;
import com.telecom.acl.service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "알림 변환 API", description = "SOAP 알림을 JSON으로 변환하여 처리하는 API")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(
            summary = "알림 테스트",
            description = """
    알림을 테스트로 발송합니다.
    
    userSequence: user1~user5 중 하나
    svcTypeCd: D(데이터), V(음성통화), S(문자메시지)
    exceedQty: 사용량
    """
    )
    @PostMapping("/test")
    public ResponseEntity<String> testNotification(@RequestBody ExcessNotificationSoap notification) {
        try {
            notificationService.processNotification(notification);
            return ResponseEntity.ok("알림이 성공적으로 처리되었습니다.");
        } catch (Exception e) {
            log.error("알림 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("알림 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "상태 확인", description = "서비스 상태 확인")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ACL Service is running");
    }
}