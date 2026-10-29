package com.nyangmeong.health.dto;

import java.time.LocalDate;

import com.nyangmeong.health.domain.Prescription;

/**
 * [처방약 응답]
 */
public record PrescriptionResponse(
        Long prscSn,
        Long ckupRcdSn,
        String mdcnNm,
        LocalDate mdcnBgngYmd,
        LocalDate mdcnEndYmd,
        Integer dayMdcnNmtm,
        String prscSttsCd,
        boolean careRfltYn) {

    public static PrescriptionResponse from(Prescription prsc) {
        return new PrescriptionResponse(prsc.getPrscSn(), prsc.getCkupRcdSn(), prsc.getMdcnNm(),
                prsc.getMdcnBgngYmd(), prsc.getMdcnEndYmd(), prsc.getDayMdcnNmtm(), prsc.getPrscSttsCd(),
                "Y".equals(prsc.getCareRfltYn()));
    }
}
