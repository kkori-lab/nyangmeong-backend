package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [오늘의 한마디 후보 문구 (TODAY_PHRS)]
 * - FN-ADM-023-04·05 : 매일 사용 중 문구 1건을 골라 CMNTY_AI_CMTRY 에 저장
 */
@Data
public class TodayPhrase {
    private Long todayPhrsSn;
    private String phrsCn;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
