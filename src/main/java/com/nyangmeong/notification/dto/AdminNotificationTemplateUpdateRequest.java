package com.nyangmeong.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 알림 템플릿 수정 요청]
 * - FN-ADM-090-02 : 세 칸 모두 덮어씀, 유형·채널은 바꾸지 않음
 */
public record AdminNotificationTemplateUpdateRequest(
        @Size(max = 200) String notiTtl,
        @NotBlank @Size(max = 4000) String notiCn,
        @NotNull Boolean useYn) {
}
