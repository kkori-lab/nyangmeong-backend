package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [검진기록 첨부 목록 행]
 * - 첨부 자료 탭·영상검사 칩 (FN-HLT-005-01·02)
 */
@Data
public class CheckupAttachmentSummary {

    /** 검진기록첨부일련번호 */
    private Long ckupRcdAtchSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 첨부유형공통코드(HLTG20) */
    private String atchTypeCd;
    /** 첨부유형명 */
    private String atchTypeCdNm;
    /** 영상종류공통코드(HLTG23) */
    private String imgKndCd;
    /** 영상종류명 */
    private String imgKndCdNm;
    /** 촬영부위공통코드(HLTG24) */
    private String shtRegnCd;
    /** 촬영부위명 */
    private String shtRegnCdNm;
    /** 원본파일명 */
    private String orgnlFileNm;
    /** 파일확장자공통코드(FILG01) */
    private String fileExtnCd;
    /** 등록일시 */
    private LocalDateTime regDt;
}
