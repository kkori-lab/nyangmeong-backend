package com.nyangmeong.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 알림 템플릿 등록 요청]
 * - FN-ADM-090-02 : notiTtl이 비면 기본 제목
 */
public record AdminNotificationTemplateCreateRequest(
        @NotBlank String notiTypeCd,
        @NotBlank String notiChnlCd,
        @Size(max = 200) String notiTtl,
        @NotBlank @Size(max = 4000) String notiCn,
        @NotNull Boolean useYn) {
}
