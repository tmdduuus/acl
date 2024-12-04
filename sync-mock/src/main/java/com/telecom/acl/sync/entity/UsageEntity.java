package com.telecom.acl.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usage_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userSequence;
    private String svcTypeCd;
    private Integer usedQty;
    private Integer baseQty;
    private String updateDtm;
}
