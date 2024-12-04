package com.telecom.acl.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcessNotificationEvent {
    private String userId;
    private String serviceType;    // VOICE, DATA, SMS
    private int usedAmount;
    private int allowance;
    private int excessAmount;
    private LocalDateTime notifyTime;
}
