// File: acl/acl-service/src/main/java/com/telecom/acl/service/config/EventHubConfig.java
package com.telecom.acl.service.config;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.telecom.acl.service.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EventHubConfig {
    @Value("${azure.eventhub.connection-string}")
    private String eventHubConnectionString;

    @Value("${azure.eventhub.name}")
    private String eventHubName;

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    @Value("${azure.storage.container}")
    private String storageContainer;

    private final NotificationService notificationService;
    private BlobServiceClient blobServiceClient;

    public EventHubConfig(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        initBlobServiceClient();
        initializeContainer();
    }

    private void initBlobServiceClient() {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
        log.info("Blob service client initialized");
    }

    private void initializeContainer() {
        try {
            createContainerIfNotExists(storageContainer);
            log.info("Blob container initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize blob container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    public void createContainerIfNotExists(String containerName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("Created blob container: {}", containerName);
            } else {
                log.info("Blob container already exists: {}", containerName);
            }
        } catch (Exception e) {
            log.error("Error creating blob container {}: {}", containerName, e.getMessage());
            throw e;
        }
    }

    @Bean
    public EventProcessorClient eventProcessorClient() {
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildAsyncClient()
                .getBlobContainerAsyncClient(storageContainer);

        log.info("Creating Event Processor Client for Event Hub: {}", eventHubName);

        return new EventProcessorClientBuilder()
                .connectionString(eventHubConnectionString, eventHubName)
                .consumerGroup("$Default")
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(notificationService::processEventHubMessage)
                .processError(context -> {
                    log.error("Error in event processor: {}",
                            context.getThrowable().getMessage(),
                            context.getThrowable());
                })
                .buildEventProcessorClient();
    }

    public BlobContainerAsyncClient getBlobContainerAsyncClient(String containerName) {
        createContainerIfNotExists(containerName);
        return new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildAsyncClient()
                .getBlobContainerAsyncClient(containerName);
    }

    public BlobContainerClient getBlobContainerClient(String containerName) {
        createContainerIfNotExists(containerName);
        return blobServiceClient.getBlobContainerClient(containerName);
    }
}