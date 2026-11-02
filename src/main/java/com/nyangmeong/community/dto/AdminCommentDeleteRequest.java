package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 댓글 삭제 요청]
 */
public record AdminCommentDeleteRequest(
        @NotBlank(message = "삭제 사유를 입력해 주세요.") @Size(max = 1000, message = "삭제 사유는 1000자 이내로 입력해 주세요.") String delRsn) {
}
