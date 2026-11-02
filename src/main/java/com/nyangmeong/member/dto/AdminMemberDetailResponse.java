package com.nyangmeong.member.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [관리자 회원 상세 기본 정보]
 * - SCR-ADM-011, 이메일·휴대전화는 마스킹 값만 응답
 */
public record AdminMemberDetailResponse(Long mbrSn, String mbrNcnm, String maskedAcntEml, String maskedMblTelno,
        String joinPathCd, List<MemberSnsAccountSummary> snsLinks, LocalDateTime regDt, LocalDateTime lastLgnDt,
        Integer hldPnt, String mbrSttsCd, LocalDateTime suspnEndDt) {
}
