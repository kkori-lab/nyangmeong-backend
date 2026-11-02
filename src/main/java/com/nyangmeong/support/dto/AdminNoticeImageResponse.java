package com.nyangmeong.support.dto;

/**
 * [공지 본문 이미지 업로드 응답]
 * - POST /api/admin/notices/images (FN-ADM-040-02)
 * - 본문에는 ![](/api/files/public/{atchFileSn}) 한 줄로 넣는다
 */
public record AdminNoticeImageResponse(Long atchFileSn) {
}
