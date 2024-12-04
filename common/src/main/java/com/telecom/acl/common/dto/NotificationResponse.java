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
@Schema(description = "알림 발송 응답")
public class NotificationResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "알림이 성공적으로 발송되었습니다.")
    private String message;

    @Schema(description = "사용자 ID", example = "user123")
    private String userId;

    @Schema(description = "알림 채널", example = "PUSH")
    private String channel;
}