package com.telecom.acl.service.config;

import com.telecom.acl.common.soap.SoapEnvelope;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JaxbConfig {

    @Bean
    public JAXBContext jaxbContext() throws JAXBException {
        try {
            log.info("Initializing JAXB context for SOAP message parsing");
            return JAXBContext.newInstance(SoapEnvelope.class);
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXB context: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public Unmarshaller unmarshaller(JAXBContext jaxbContext) throws JAXBException {
        try {
            log.info("Creating JAXB unmarshaller");
            return jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            log.error("Failed to create JAXB unmarshaller: {}", e.getMessage(), e);
            throw e;
        }
    }
}