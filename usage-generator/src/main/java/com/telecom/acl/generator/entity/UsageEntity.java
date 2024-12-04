package com.telecom.acl.generator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(name = "id")
    private Long id;

    @Column(name = "user_sequence")
    private String userSequence;

    @Column(name = "svc_type_cd")
    private String svcTypeCd;

    @Column(name = "used_qty")
    private Integer usedQty;

    @Column(name = "base_qty")
    private Integer baseQty;

    @Column(name = "update_dtm")
    private String updateDtm;
}
