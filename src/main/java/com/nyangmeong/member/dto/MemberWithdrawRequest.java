package com.nyangmeong.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * [회원탈퇴 요청]
 * - POST /api/members/me/withdraw (FN-ACC-020-03·14·20)
 * - 확인 체크와 입력 문구는 요청 검증에만 쓰고 저장하지 않는다. 문구 비교는 서비스가 한다
 */
public record MemberWithdrawRequest(
        @NotNull(message = "탈퇴 유의사항을 확인해 주세요.") @AssertTrue(message = "탈퇴 유의사항을 확인해 주세요.") Boolean noticeConfirmed,
        @NotNull(message = "탈퇴 확인 문구를 입력해 주세요.") String confirmPhrase) {

    @Override
    public String toString() {
        return "MemberWithdrawRequest[noticeConfirmed=" + noticeConfirmed + "]";
    }
}
