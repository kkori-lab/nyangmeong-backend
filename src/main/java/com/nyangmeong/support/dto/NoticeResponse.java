package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import com.nyangmeong.support.domain.Notice;

/**
 * [공지사항 상세 응답]
 * - GET /api/notices/{ntcMttrSn} (FN-CSC-001-05)
 */
public record NoticeResponse(
        Long ntcMttrSn,
        String ntcMttrTtl,
        String ntcMttrCn,
        LocalDateTime regDt) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(notice.getNtcMttrSn(), notice.getNtcMttrTtl(), notice.getNtcMttrCn(),
                notice.getRegDt());
    }
}
