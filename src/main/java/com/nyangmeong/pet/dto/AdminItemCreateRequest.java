package com.nyangmeong.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 아이템 등록 요청]
 * - multipart/form-data (FN-ADM-070-02)
 * - bakeHint 선택, 신발은 넣을 수 없음
 * - bakeImgRefYn 선택(없으면 false), true 면 아이템 그림을 참고로 굽는다
 */
public record AdminItemCreateRequest(
        @NotBlank String itemRegnCd,
        @NotBlank @Size(max = 50) String itemNm,
        @NotNull @Min(0) @Max(1_000_000) Long needPnt,
        @NotNull Boolean ntslYn,
        @Size(max = 300) String bakeHint,
        Boolean bakeImgRefYn,
        @NotNull MultipartFile image) {
}
