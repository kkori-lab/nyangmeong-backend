package com.nyangmeong.community.dto;

import com.nyangmeong.community.domain.TodayPhrase;

/**
 * [관리자 오늘의 한마디 후보 문구 항목]
 * - FN-ADM-023-04
 */
public record AdminTodayPhraseSummary(Long todayPhrsSn, String phrsCn, Boolean useYn) {

    public static AdminTodayPhraseSummary from(TodayPhrase phrase) {
        return new AdminTodayPhraseSummary(phrase.getTodayPhrsSn(), phrase.getPhrsCn(),
                "Y".equals(phrase.getUseYn()));
    }
}
