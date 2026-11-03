package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [병원 일정 알림 대상 행]
 * - 케어 일정 사전 알림 배치용 (FN-AIM-006-02) : 삭제된 일정·반려동물 제외
 */
@Data
public class HospitalScheduleSummary {

    /** 일정일련번호 */
    private Long schdlSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 반려동물명 */
    private String petNm;
    /** 일정일시 */
    private LocalDateTime schdlDt;
    /** 시간입력여부 */
    private boolean hrInptYn;
    /** 병원명 */
    private String hsptlNm;
    /** 검진유형명 */
    private String ckupTypeNm;
}
