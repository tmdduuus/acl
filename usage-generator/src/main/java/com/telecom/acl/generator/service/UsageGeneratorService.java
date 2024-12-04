package com.telecom.acl.generator.service;

import com.telecom.acl.generator.entity.UsageEntity;
import com.telecom.acl.generator.repository.UsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageGeneratorService {
    private final UsageRepository usageRepository;
    private final Random random = new Random();
    private final String[] users = {"user1", "user2", "user3", "user4", "user5"};
    private final String[] services = {"V", "D", "S"};
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Scheduled(fixedRate = 30000)  // 30초마다 실행
    public void generateUsageData() {
        String user = users[random.nextInt(users.length)];
        String svc = services[random.nextInt(services.length)];
        int baseQty = svc.equals("D") ? 10240 : 300;
        int usedQty = baseQty - random.nextInt(100);

        log.info("### Send usage data ###");
        log.info("user: {}", user);
        log.info("service: {} (Voice, Data, SMS)", svc);
        log.info("기본량: {}", baseQty);
        log.info("사용량: {}", usedQty);

        UsageEntity data = UsageEntity.builder()
                .userSequence(user)
                .svcTypeCd(svc)
                .baseQty(baseQty)
                .usedQty(usedQty)
                .updateDtm(LocalDateTime.now().format(dtf))
                .build();

        usageRepository.save(data);
        log.info("Generated usage data: {}", data);
    }
}
