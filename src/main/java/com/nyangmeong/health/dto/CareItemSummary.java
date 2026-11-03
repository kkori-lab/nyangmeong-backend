package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [케어 항목 목록 행]
 * - 토탈 케어리스트·메인 표시 항목 (FN-HLT-003-12·13)
 */
@Data
public class CareItemSummary {

    /** 케어항목일련번호 */
    private Long careArtclSn;
    /** 케어항목명 */
    private String careArtclNm;
    /** 케어구역공통코드(HLTG13) */
    private String careZoneCd;
    /** 케어주기공통코드(HLTG15) */
    private String careCycleCd;
    /** 케어주기명 */
    private String careCycleCdNm;
    /** 주기직접입력일수 */
    private Integer cycleDrctInptDayCnt;
    /** 대표아이콘ID */
    private String rprsIconId;
    /** 차기예정일자 */
    private LocalDate nxtmPrnmntYmd;
    /** 최종완료일자 */
    private LocalDate lastCmptnYmd;
    /** 메인노출여부 */
    private boolean mainExpsrYn;
    /** 메인정렬순서 */
    private Integer mainSortSeq;
    /** 기본항목여부 */
    private boolean bscArtclYn;
    /** 처방일련번호 */
    private Long prscSn;
    /** 반복예방관리일련번호 */
    private Long reptPrvntMngSn;
    /** 출처 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 복용종료일자 */
    private LocalDate mdcnEndYmd;
    /** 1일복용횟수 */
    private Integer dayMdcnNmtm;
    /** 오늘 완료 건수 */
    private Integer todayCmptnCnt;
}
