package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [케어 항목 VO]
 * - CARE_ARTCL 1:1
 */
@Data
public class CareItem {

    /** 케어항목일련번호 */
    private Long careArtclSn;
    /** 처방일련번호 */
    private Long prscSn;
    /** 반복예방관리일련번호 */
    private Long reptPrvntMngSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 케어구역공통코드(HLTG13) */
    private String careZoneCd;
    /** 케어주기공통코드(HLTG15) */
    private String careCycleCd;
    /** 주기직접입력일수 */
    private Integer cycleDrctInptDayCnt;
    /** 케어항목명 */
    private String careArtclNm;
    /** 대표아이콘ID */
    private String rprsIconId;
    /** 차기예정일자 */
    private LocalDate nxtmPrnmntYmd;
    /** 최종완료일자 */
    private LocalDate lastCmptnYmd;
    /** 메인노출여부 */
    private String mainExpsrYn;
    /** 메인정렬순서 */
    private Integer mainSortSeq;
    /** 기본항목여부 */
    private String bscArtclYn;
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
