package com.telecom.acl.service.service;

import com.telecom.acl.common.dto.NotificationRequest;
import com.telecom.acl.common.dto.NotificationResponse;
import com.telecom.acl.common.exception.NotificationException;
import com.telecom.acl.common.soap.ExcessNotificationSoap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class NotificationSender {
    private final RestTemplate restTemplate;

    @Value("${notification.mock.url}")
    private String notificationUrl;

    public NotificationSender(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendToAllChannels(ExcessNotificationSoap soap) {
        sendNotification(soap, "PUSH");
        sendNotification(soap, "SMS");
        sendNotification(soap, "KAKAO");
    }

    private void sendNotification(ExcessNotificationSoap soap, String channel) {
        try {
            NotificationRequest request = createNotificationRequest(soap, channel);
            NotificationResponse response = restTemplate.postForObject(
                    notificationUrl + "/api/notifications/send",
                    request,
                    NotificationResponse.class
            );
            log.info("Notification sent via {}: {}", channel, response);
        } catch (Exception e) {
            log.error("Error sending {} notification: {}", channel, e.getMessage(), e);
            throw new NotificationException(channel + " 알림 발송 실패", e);
        }
    }

    private NotificationRequest createNotificationRequest(ExcessNotificationSoap soap, String channel) {
        return NotificationRequest.builder()
                .userId(soap.getUserSequence())
                .channel(channel)
                .message(createNotificationMessage(soap))
                .build();
    }

    private String createNotificationMessage(ExcessNotificationSoap soap) {
        String unit = getUnitByServiceType(soap.getSvcTypeCd());
        return String.format("[%s] %s 기본 제공량을 %d%s 초과하였습니다.",
                convertServiceType(soap.getSvcTypeCd()),
                soap.getUserSequence(),
                soap.getExceedQty(),
                unit);
    }

    private String getUnitByServiceType(String svcTypeCd) {
        return switch (svcTypeCd) {
            case "D" -> "MB";
            case "V" -> "분";
            case "S" -> "건";
            default -> throw new IllegalArgumentException("Unknown service type: " + svcTypeCd);
        };
    }

    private String convertServiceType(String svcTypeCd) {
        return switch (svcTypeCd) {
            case "V" -> "VOICE";
            case "D" -> "DATA";
            case "S" -> "SMS";
            default -> throw new IllegalArgumentException("Unknown service type: " + svcTypeCd);
        };
    }
}