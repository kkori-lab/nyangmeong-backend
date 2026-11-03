package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [반복 예방 목록 행]
 * - 반복 예방 탭 (FN-HLT-005-05)
 */
@Data
public class RepeatPreventionSummary {

    /** 반복예방관리일련번호 */
    private Long reptPrvntMngSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 병원명 (반려동물 전체 조회에서만, 출처 검진기록) */
    private String hsptlNm;
    /** 반복예방항목공통코드(HLTG07) */
    private String reptPrvntArtclCd;
    /** 반복예방항목명 */
    private String reptPrvntArtclCdNm;
    /** 반복주기공통코드(HLTG15) */
    private String reptCycleCd;
    /** 반복주기명 */
    private String reptCycleCdNm;
    /** 주기직접입력일수 */
    private Integer cycleDrctInptDayCnt;
    /** 시작일자 */
    private LocalDate bgngYmd;
    /** 다음 예정일 */
    private LocalDate nxtmPrnmntYmd;
}
