package com.nyangmeong.member.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * [기존 정보 복구 안내 응답]
 * - POST /api/members/recovery-info, 본인확인된 탈퇴 보관 회원의 복구 대상 요약
 * - maskedMblTelno 는 보관 번호가 없으면 빠지고(직접 입력), missingTrms 는 다시 받아야 할 현재 필수 약관
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberRecoveryResponse(String acntEml, String mbrNcnm, LocalDateTime whdwlDt,
        LocalDateTime rcvryExpryDt, String maskedMblTelno, List<TermsSummary> missingTrms) {
}
