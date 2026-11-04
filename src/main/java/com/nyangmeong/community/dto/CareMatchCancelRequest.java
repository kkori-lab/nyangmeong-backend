package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [매칭 요청 취소]
 * - 사유는 칩 선택 문구 또는 직접 입력 50자
 */
public record CareMatchCancelRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String cnclRsn) {
}
