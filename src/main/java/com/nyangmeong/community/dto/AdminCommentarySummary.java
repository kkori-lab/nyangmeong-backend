package com.nyangmeong.community.dto;

import java.time.LocalDate;

import com.nyangmeong.community.domain.CommunityAiCommentary;

/**
 * [관리자 AI 코멘트 항목]
 * - FN-ADM-023-06 : 사용 안 함 포함
 */
public record AdminCommentarySummary(Long cmntyAiCmtrySn, LocalDate cmtryCrtrYmd, String cmtryCn, Boolean useYn) {

    public static AdminCommentarySummary from(CommunityAiCommentary commentary) {
        return new AdminCommentarySummary(commentary.getCmntyAiCmtrySn(), commentary.getCmtryCrtrYmd(),
                commentary.getCmtryCn(), "Y".equals(commentary.getUseYn()));
    }
}
