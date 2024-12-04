// File: acl/kos-mock/src/main/java/com/telecom/acl/kos/service/NotificationGenerator.java
package com.telecom.acl.kos.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.telecom.acl.common.soap.ExcessNotificationSoap;
import com.telecom.acl.common.soap.SoapEnvelope;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class NotificationGenerator {
    private final EventHubProducerClient eventHubClient;
    private final Random random = new Random();
    private final String[] users = {"user1", "user2", "user3", "user4", "user5"};
    private final String[] serviceTypes = {"V", "D", "S"};  // V:음성, D:데이터, S:문자
    private final JAXBContext jaxbContext;

    public NotificationGenerator(EventHubProducerClient eventHubClient) throws JAXBException {
        this.eventHubClient = eventHubClient;
        this.jaxbContext = JAXBContext.newInstance(SoapEnvelope.class);
    }

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void generateNotification() {
        try {
            ExcessNotificationSoap notification = createRandomSoapNotification();
            String soapMessage = convertToSoapXml(notification);
            publishEvent(soapMessage);
            log.info("Generated and published notification for user: {}", notification.getUserSequence());
        } catch (Exception e) {
            log.error("Error generating notification: {}", e.getMessage(), e);
        }
    }

    private ExcessNotificationSoap createRandomSoapNotification() {
        String userSequence = users[random.nextInt(users.length)];
        String svcTypeCd = serviceTypes[random.nextInt(serviceTypes.length)];
        int baseQty = (svcTypeCd.equals("D")) ? 10240 : 300; // DATA는 MB단위
        int usedQty = baseQty + random.nextInt(100);

        return ExcessNotificationSoap.builder()
                .userSequence(userSequence)
                .svcTypeCd(svcTypeCd)
                .usedQty(usedQty)
                .baseQty(baseQty)
                .exceedQty(usedQty - baseQty)
                .notifyDtm(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .build();
    }

    private String convertToSoapXml(ExcessNotificationSoap notification) throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        SoapEnvelope envelope = new SoapEnvelope(new SoapEnvelope.SoapBody(notification));

        StringWriter sw = new StringWriter();
        marshaller.marshal(envelope, sw);
        return sw.toString();
    }

    private void publishEvent(String soapMessage) {
        try {
            EventData eventData = new EventData(soapMessage);
            List<EventData> events = Arrays.asList(eventData);  // 단일 이벤트를 List로 변환
            eventHubClient.send(events);  // Iterable<EventData> 형태로 전송
            log.debug("Published SOAP message: {}", soapMessage);
        } catch (Exception e) {
            log.error("Error publishing event: {}", e.getMessage(), e);
        }
    }
}