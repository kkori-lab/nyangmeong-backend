package com.nyangmeong.community.dto;

/**
 * [오늘의 건강팁 노출 자리]
 * - FN-ADM-023-01·03 : 관리자 지정 글은 hlthInfoExpsrSeq 값, 자동 선정 글은 null
 */
public record HealthTipSlot(Integer hlthInfoExpsrSeq, PostSummary post) {
}
