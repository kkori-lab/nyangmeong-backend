package com.nyangmeong.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [관리자 아이템 수정 요청]
 * - multipart/form-data, 보낸 칸만 바뀜 (FN-ADM-070-03)
 * - bakeHint 빈 문자열이면 굽기 지시 지움, image 는 바꿀 때만
 */
public record AdminItemUpdateRequest(
        String itemRegnCd,
        @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String itemNm,
        @Min(0) @Max(1_000_000) Long needPnt,
        Boolean ntslYn,
        @Size(max = 300) String bakeHint,
        MultipartFile image) {
}
