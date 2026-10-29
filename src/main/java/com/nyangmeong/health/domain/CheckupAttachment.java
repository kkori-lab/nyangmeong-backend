package com.nyangmeong.health.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [검진기록 첨부 VO]
 * - CKUP_RCD_ATCH 1:1
 */
@Data
public class CheckupAttachment {

    /** 검진기록첨부일련번호 */
    private Long ckupRcdAtchSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 첨부파일일련번호 */
    private Long atchFileSn;
    /** 첨부유형공통코드(HLTG20) */
    private String atchTypeCd;
    /** 영상종류공통코드(HLTG23) */
    private String imgKndCd;
    /** 촬영부위공통코드(HLTG24) */
    private String shtRegnCd;
    /** 삭제여부 */
    private String delYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
