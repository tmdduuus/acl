package com.telecom.acl.sync.repository;

import com.telecom.acl.sync.entity.UsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageRepository extends JpaRepository<UsageEntity, Long> {
    @Query("SELECT u FROM UsageEntity u WHERE u.updateDtm > :lastSyncTime")
    List<UsageEntity> findUpdatedAfter(@Param("lastSyncTime") String lastSyncTime);
}
