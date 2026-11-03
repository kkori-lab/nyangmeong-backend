package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

/**
 * [검진기록 첨부 응답]
 * - 목록 행(CheckupAttachmentSummary)과 같은 필드
 */
public record CheckupAttachmentResponse(
        Long ckupRcdAtchSn,
        Long ckupRcdSn,
        String atchTypeCd,
        String atchTypeCdNm,
        String imgKndCd,
        String imgKndCdNm,
        String shtRegnCd,
        String shtRegnCdNm,
        String orgnlFileNm,
        String fileExtnCd,
        LocalDateTime regDt) {

    public static CheckupAttachmentResponse from(CheckupAttachmentSummary atch) {
        return new CheckupAttachmentResponse(atch.getCkupRcdAtchSn(), atch.getCkupRcdSn(), atch.getAtchTypeCd(),
                atch.getAtchTypeCdNm(), atch.getImgKndCd(), atch.getImgKndCdNm(), atch.getShtRegnCd(),
                atch.getShtRegnCdNm(), atch.getOrgnlFileNm(), atch.getFileExtnCd(), atch.getRegDt());
    }
}
