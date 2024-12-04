package com.telecom.acl.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UsageGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsageGeneratorApplication.class, args);
    }
}
