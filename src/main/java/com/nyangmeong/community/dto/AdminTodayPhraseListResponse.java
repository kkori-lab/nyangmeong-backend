package com.nyangmeong.community.dto;

import java.util.List;

/**
 * [관리자 오늘의 한마디 목록 응답]
 * - FN-ADM-023-04·05 : todayPhrs 는 오늘 노출 중인 문구(없으면 null), 같은 문구의 후보가 없으면 todayPhrsSn null
 */
public record AdminTodayPhraseListResponse(TodayPhrs todayPhrs, List<AdminTodayPhraseSummary> content) {

    /** 오늘 노출 문구 */
    public record TodayPhrs(Long todayPhrsSn, String phrsCn) {
    }
}
