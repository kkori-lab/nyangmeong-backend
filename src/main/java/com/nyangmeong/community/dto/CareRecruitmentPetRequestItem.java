package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [펫돌봄 모집 반려동물 요청 항목]
 * - 등록·수정 요청이 함께 쓴다
 * - vrtySn·vrtyDrctInptNm 중 하나만 보낸다, petSizeCd(SMALL·MEDIUM·LARGE)는 강아지면 필수
 */
public record CareRecruitmentPetRequestItem(
        @NotBlank String petSeCd,
        Long vrtySn,
        @Size(max = 20) String vrtyDrctInptNm,
        String petSizeCd) {
}
