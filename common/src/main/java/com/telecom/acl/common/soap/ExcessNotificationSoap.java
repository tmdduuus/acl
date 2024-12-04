package com.telecom.acl.common.soap;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "ExcessNotification", namespace = "http://kos.telecom.com/notifications")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcessNotificationSoap {
    @XmlElement(required = true)
    private String userSequence;  // Legacy 시스템의 사용자 ID 형식

    @XmlElement(required = true)
    private String svcTypeCd;     // Legacy 시스템의 서비스 타입 코드 (V:음성, D:데이터, S:문자)

    @XmlElement(required = true)
    private Integer usedQty;      // 사용량

    @XmlElement(required = true)
    private Integer baseQty;      // 기본 제공량

    @XmlElement(required = true)
    private Integer exceedQty;    // 초과량

    @XmlElement(required = true)
    private String notifyDtm;     // Legacy 형식 날짜시간 (yyyyMMddHHmmss)
}