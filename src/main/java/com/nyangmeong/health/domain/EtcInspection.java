package com.nyangmeong.health.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [기타검사 VO]
 * - ETC_INSP 1:1
 */
@Data
public class EtcInspection {

    /** 기타검사일련번호 */
    private Long etcInspSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 검사명 */
    private String inspNm;
    /** 검사결과내용 */
    private String inspRsltCn;
    /** 메모내용 */
    private String memoCn;
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
