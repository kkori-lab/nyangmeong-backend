package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [반복예방관리 VO]
 * - REPT_PRVNT_MNG 1:1
 */
@Data
public class RepeatPrevention {

    /** 반복예방관리일련번호 */
    private Long reptPrvntMngSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 반복예방항목공통코드(HLTG07) */
    private String reptPrvntArtclCd;
    /** 반복주기공통코드(HLTG15) */
    private String reptCycleCd;
    /** 시작일자 */
    private LocalDate bgngYmd;
    /** 주기직접입력일수 */
    private Integer cycleDrctInptDayCnt;
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
