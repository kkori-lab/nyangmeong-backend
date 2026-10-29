package com.nyangmeong.health.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * [검진기록 첨부 수정 요청]
 * - multipart/form-data : 보낸 항목만 수정, 파일은 이 첨부 1건만 교체
 */
public record CheckupAttachmentUpdateRequest(
        MultipartFile file,
        String atchTypeCd,
        String imgKndCd,
        String shtRegnCd) {
}
