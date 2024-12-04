package com.telecom.acl.generator.repository;

import com.telecom.acl.generator.entity.UsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRepository extends JpaRepository<UsageEntity, Long> {
}
