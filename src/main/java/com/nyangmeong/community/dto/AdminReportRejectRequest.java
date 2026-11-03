package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 신고 반려 요청]
 */
public record AdminReportRejectRequest(
        @NotBlank(message = "반려 사유를 입력해 주세요.") @Size(max = 1000, message = "반려 사유는 1000자 이내로 입력해 주세요.") String rjctRsn) {
}
