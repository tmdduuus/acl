package com.telecom.acl.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SyncMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(SyncMockApplication.class, args);
    }
}
