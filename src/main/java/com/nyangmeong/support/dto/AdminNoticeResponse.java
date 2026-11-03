package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import com.nyangmeong.support.domain.Notice;

/**
 * [관리자 공지 상세 응답]
 * - GET·PATCH /api/admin/notices/{ntcMttrSn}, POST /api/admin/notices (FN-ADM-040-02~04)
 * - notiSndngDt : null 이면 미발송
 */
public record AdminNoticeResponse(
        Long ntcMttrSn,
        String ntcMttrTtl,
        String ntcMttrCn,
        boolean upendFixYn,
        boolean useYn,
        Integer inqCnt,
        LocalDateTime notiSndngDt,
        Integer notiTrgtCnt,
        LocalDateTime regDt,
        LocalDateTime mdfcnDt) {

    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(notice.getNtcMttrSn(), notice.getNtcMttrTtl(), notice.getNtcMttrCn(),
                "Y".equals(notice.getUpendFixYn()), "Y".equals(notice.getUseYn()), notice.getInqCnt(),
                notice.getNotiSndngDt(), notice.getNotiTrgtCnt(), notice.getRegDt(), notice.getMdfcnDt());
    }
}
