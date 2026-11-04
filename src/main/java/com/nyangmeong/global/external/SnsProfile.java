package com.nyangmeong.global.external;

/**
 * [SNS 제공자가 준 회원 정보]
 * - accountId : 제공자 고유 계정값 (SNS_ACNT_LINK.OFFR_ACNT_ID)
 * - email·mblTelno : 제공자가 주지 않았으면 null, mblTelno 는 제공자 표기 그대로
 * - ageCnfrmd : 제공자 쪽에서 만 14세 이상 확인이 끝났는지 (지금은 연령을 받아 오지 않아 항상 false)
 */
public record SnsProfile(SnsProvider provider, String accountId, String email, String mblTelno, boolean ageCnfrmd) {
}
