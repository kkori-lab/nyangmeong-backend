package com.nyangmeong.community.dto;

/**
 * [관리자 오늘의 건강팁 현황 항목]
 * - FN-ADM-023-01 : 노출 자리 순서, 지정 글은 hlthInfoExpsrSeq 값, 자동 선정은 null
 */
public record AdminHealthTipResponse(Long pstSn, String pstTtl, Integer hlthInfoExpsrSeq) {

    public static AdminHealthTipResponse from(HealthTipSlot slot) {
        return new AdminHealthTipResponse(slot.post().getPstSn(), slot.post().getPstTtl(), slot.hlthInfoExpsrSeq());
    }
}
