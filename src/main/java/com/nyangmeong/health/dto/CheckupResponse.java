package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [검진기록 응답]
 * - 등록·수정 응답, 소유 확인용 조회
 */
@Data
public class CheckupResponse {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 일정일련번호 */
    private Long schdlSn;
    /** 검진일 */
    private LocalDate ckupYmd;
    /** 병원명 */
    private String hsptlNm;
    /** 검진유형공통코드 */
    private String ckupTypeCd;
    /** 검진유형명 */
    private String ckupTypeCdNm;
    /** 검진내용 */
    private String ckupCn;
    /** 검진메모내용 */
    private String ckupMemoCn;
    /** 영수증일련번호 */
    private Long recptSn;
}
