package com.telecom.acl.kos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KosMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(KosMockApplication.class, args);
    }
}
