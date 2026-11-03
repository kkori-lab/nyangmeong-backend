package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [케어 완료 이력 VO]
 * - CARE_CMPTN_HSTRY 1:1
 */
@Data
public class CareCompletion {

    /** 케어완료이력일련번호 */
    private Long careCmptnHstrySn;
    /** 케어항목일련번호 */
    private Long careArtclSn;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 완료회차 */
    private Integer cmptnCycl;
    /** 완료일자 */
    private LocalDate cmptnYmd;
    /** 삭제여부 */
    private String delYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 수정자ID */
    private String mdfrId;
}
