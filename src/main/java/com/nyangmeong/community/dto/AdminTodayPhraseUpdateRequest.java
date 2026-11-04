package com.nyangmeong.community.dto;

/**
 * [관리자 오늘의 한마디 후보 문구 수정 요청]
 * - FN-ADM-023-04 : 보낸 칸만 바꿈, 둘 다 없으면 400
 */
public record AdminTodayPhraseUpdateRequest(String phrsCn, Boolean useYn) {
}
