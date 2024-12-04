package com.telecom.acl.sync.service;

import com.telecom.acl.sync.entity.UsageEntity;
import com.telecom.acl.sync.repository.UsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {
    private final UsageRepository usageRepository;
    private final MongoTemplate mongoTemplate;
    private String lastSyncTime = "20240101000000";
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Scheduled(fixedRate = 30000)  // 30초마다 실행
    public void syncData() {
        try {
            List<UsageEntity> updatedData = usageRepository.findUpdatedAfter(lastSyncTime);

            if (!updatedData.isEmpty()) {
                updatedData.forEach(this::updatePhonePlanView);
                lastSyncTime = LocalDateTime.now().format(dtf);
                log.info("Synced {} records. Last sync time: {}", updatedData.size(), lastSyncTime);
            }
        } catch (Exception e) {
            log.error("Error during sync: {}", e.getMessage(), e);
        }
    }

    private void updatePhonePlanView(UsageEntity data) {
        try {
            Query query = new Query(Criteria.where("userId").is(data.getUserSequence()));
            Update update = new Update();

            switch (data.getSvcTypeCd()) {
                case "V":
                    update.set("callUsage", data.getUsedQty());
                    update.set("callMinutes", data.getBaseQty());
                    break;
                case "D":
                    update.set("dataUsage", data.getUsedQty());
                    update.set("dataAllowance", data.getBaseQty());
                    break;
                case "S":
                    update.set("messageUsage", data.getUsedQty());
                    update.set("messageCount", data.getBaseQty());
                    break;
                default:
                    log.warn("Unknown service type: {}", data.getSvcTypeCd());
                    return;
            }

            mongoTemplate.upsert(query, update, "phone_plan_views");
            log.debug("Updated user {}'s {} usage", data.getUserSequence(), data.getSvcTypeCd());
        } catch (Exception e) {
            log.error("Error updating MongoDB: {}", e.getMessage(), e);
        }
    }
}
