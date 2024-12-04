package com.telecom.acl.notification.controller;

import com.telecom.acl.common.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "알림 발송 Mock API", description = "알림 발송을 시뮬레이션하는 Mock API")
public class NotificationController {

    @Operation(summary = "알림 발송", description = "각 채널별 알림 발송을 시뮬레이션")
    @PostMapping("/send")
    public NotificationResponse sendNotification(@RequestBody NotificationRequest request) {
        log.info("Notification request received: {}", request);
        return NotificationResponse.builder()
                .success(true)
                .message("알림이 발송되었습니다.")
                .userId(request.userId())
                .channel(request.channel())
                .build();
    }

    private record NotificationRequest(String userId, String channel, String message) {}
}
