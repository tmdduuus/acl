package com.telecom.acl.service.service;

import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.acl.common.dto.NotificationRequest;
import com.telecom.acl.common.dto.NotificationResponse;
import com.telecom.acl.common.exception.NotificationException;
import com.telecom.acl.common.soap.ExcessNotificationSoap;
import jakarta.xml.bind.JAXBContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class NotificationService {
    private final RestTemplate restTemplate;
    private final SoapParserService soapParserService;
    private final NotificationSender notificationSender;

    @Value("${notification.mock.url}")
    private String notificationUrl;

    public NotificationService(
            RestTemplate restTemplate,
            SoapParserService soapParserService,
            NotificationSender notificationSender) {
        this.restTemplate = restTemplate;
        this.soapParserService = soapParserService;
        this.notificationSender = notificationSender;
    }

    public void processEventHubMessage(EventContext eventContext) {
        try {
            String partitionId = eventContext.getPartitionContext().getPartitionId();
            log.info("Received event from partition {}", partitionId);

            String soapXml = eventContext.getEventData().getBodyAsString();
            log.info("Received SOAP XML from partition {}: {}", partitionId, soapXml);

            // 메시지 속성 로깅
            eventContext.getEventData().getProperties().forEach((key, value) ->
                    log.debug("Event property - {}: {}", key, value));

            ExcessNotificationSoap soapNotification = soapParserService.parseSoapXml(soapXml);
            log.info("Successfully parsed SOAP message for user: {}", soapNotification.getUserSequence());

            processNotification(soapNotification);

            // 체크포인트 갱신 전 로깅
            log.info("Updating checkpoint for partition {} after processing message", partitionId);
            eventContext.updateCheckpoint();
            log.info("Successfully updated checkpoint for partition {}", partitionId);

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
            throw new NotificationException("이벤트 처리 중 오류 발생", e);
        }
    }

    public void processNotification(ExcessNotificationSoap soap) {
        try {
            notificationSender.sendToAllChannels(soap);
            log.info("All notifications sent successfully for user: {}", soap.getUserSequence());
        } catch (Exception e) {
            log.error("Error in notification process: {}", e.getMessage(), e);
            throw new NotificationException("알림 처리 중 오류 발생", e);
        }
    }
}