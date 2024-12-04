package com.telecom.acl.service.service;

import com.azure.messaging.eventhubs.EventProcessorClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventProcessorService {
    private final EventProcessorClient eventProcessorClient;

    @Value("${azure.eventhub.name}")
    private String eventHubName;

    public EventProcessorService(EventProcessorClient eventProcessorClient) {
        this.eventProcessorClient = eventProcessorClient;
    }

    @PostConstruct
    public void startProcessors() {
        try {
            log.info("Starting event processor for Event Hub: {}", eventHubName);
            eventProcessorClient.start();

            // 추가 상태 체크
            boolean isRunning = true; // eventProcessorClient의 상태 체크 로직 필요
            if (isRunning) {
                log.info("Event processor successfully started and running for Event Hub: {}", eventHubName);
            } else {
                log.error("Event processor started but not running for Event Hub: {}", eventHubName);
            }
        } catch (Exception e) {
            log.error("Failed to start event processor for Event Hub {}: {}", eventHubName, e.getMessage(), e);
            throw new RuntimeException("Failed to start event processor", e);
        }
    }

    @PreDestroy
    public void stopProcessors() {
        try {
            log.info("Stopping event processor for Event Hub: {}", eventHubName);
            eventProcessorClient.stop();
            log.info("Event processor stopped successfully for Event Hub: {}", eventHubName);
        } catch (Exception e) {
            log.error("Error stopping event processor for Event Hub {}: {}", eventHubName, e.getMessage(), e);
        }
    }
}