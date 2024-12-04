package com.telecom.acl.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 발송 요청")
public class NotificationRequest {
    @Schema(description = "사용자 ID", example = "user123")
    private String userId;

    @Schema(description = "알림 채널", example = "PUSH", allowableValues = {"PUSH", "SMS", "KAKAO"})
    private String channel;

    @Schema(description = "알림 메시지", example = "[DATA] 데이터 기본 제공량을 100MB 초과하였습니다.")
    private String message;
}
