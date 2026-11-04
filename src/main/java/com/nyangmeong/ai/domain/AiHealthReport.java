package com.nyangmeong.ai.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [AI 건강리포트 VO]
 * - AI_HLTH_RPT : 반려동물당 1행 덮어쓰기
 */
@Data
public class AiHealthReport {

    /** AI건강리포트일련번호 */
    private Long aiHlthRptSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 기준일자 */
    private LocalDate baseYmd;
    /** 리포트제목 */
    private String rptTtl;
    /** 분석내용 (팁 문자열 JSON 배열) */
    private String anlsCn;
    /** 입력해시 */
    private String inptHash;
    /** 새로고침횟수 (기준일 하루) */
    private Integer rfrshNmtm;
    /** 자동재생성횟수 (기준일 하루) */
    private Integer autoRgnrNmtm;
    /** 생성일시 */
    private LocalDateTime crtDt;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
