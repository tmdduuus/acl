package com.telecom.acl.service.service;

import com.telecom.acl.common.exception.NotificationException;
import com.telecom.acl.common.soap.ExcessNotificationSoap;
import com.telecom.acl.common.soap.SoapEnvelope;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Slf4j
@Service
public class SoapParserService {

    private final Unmarshaller unmarshaller;

    public SoapParserService(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public ExcessNotificationSoap parseSoapXml(String soapXml) {
        try {
            log.debug("Parsing SOAP XML: {}", soapXml);

            SoapEnvelope envelope = (SoapEnvelope) unmarshaller.unmarshal(new StringReader(soapXml));

            if (envelope == null || envelope.getBody() == null || envelope.getBody().getNotification() == null) {
                throw new NotificationException("Invalid SOAP message structure");
            }

            ExcessNotificationSoap notification = envelope.getBody().getNotification();
            log.debug("Successfully parsed SOAP notification: {}", notification);

            return notification;

        } catch (JAXBException e) {
            log.error("SOAP XML 파싱 실패: {}", e.getMessage(), e);
            throw new NotificationException("SOAP XML 파싱 실패", e);
        }
    }
}
