package com.nyangmeong.pet.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [공동양육 초대 발송 요청]
 * - 이메일 검증은 회원가입 이메일과 같음
 */
public record CaregiverInvitationCreateRequest(
        @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String invtnTrgtEml,
        @NotEmpty(message = "초대할 반려동물을 골라 주세요.") List<@NotNull Long> petSns) {
}
