package com.nyangmeong.support.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [관리자 공지 수정 요청]
 * - PATCH /api/admin/notices/{ntcMttrSn} (FN-ADM-040-04·05)
 * - 모두 선택, 보내지 않은 값(null)은 그대로
 * - useYn=false 면 비노출 (삭제 대신)
 */
public record AdminNoticeUpdateRequest(
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.") String ntcMttrTtl,
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "내용을 입력해 주세요.") @Size(max = 4000, message = "내용은 4000자 이내로 입력해 주세요.") String ntcMttrCn,
        Boolean upendFixYn,
        Boolean useYn) {
}
