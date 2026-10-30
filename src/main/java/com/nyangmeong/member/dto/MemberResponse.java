package com.nyangmeong.member.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [회원 응답]
 * - POST /api/members, GET·PATCH /api/members/me, PATCH /api/members/me/nickname
 * - 이메일·휴대전화번호는 복호화한 값
 * - joinDt·lastLgnDt 는 회원정보 수정의 계정 정보(가입일·최근 로그인, FN-MYP-040-13), 첫 로그인 전이면 lastLgnDt 는 null
 */
public record MemberResponse(String acntEml, String mbrNcnm, String mblTelno, Long atchFileSn, String joinPathCd,
        boolean ncnmStngCmptnYn, List<MemberSnsAccountSummary> snsAcntLinks, LocalDateTime joinDt,
        LocalDateTime lastLgnDt) {
}
