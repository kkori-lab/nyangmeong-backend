package com.nyangmeong.pet.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * [공동양육 초대 수락 요청]
 * - 고르지 않은 반려동물 = 거절
 */
public record CaregiverInvitationAcceptRequest(
        @NotEmpty(message = "함께 돌볼 반려동물을 골라 주세요.") List<@NotNull Long> petSns) {
}
