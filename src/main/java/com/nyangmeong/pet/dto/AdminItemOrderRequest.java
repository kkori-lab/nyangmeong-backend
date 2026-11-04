package com.nyangmeong.pet.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 아이템 순서 저장 요청]
 * - FN-ADM-070-04
 * - 그 부위 아이템 전부(판매 중지 포함)를 상점 순서대로
 */
public record AdminItemOrderRequest(@NotBlank String itemRegnCd, @NotEmpty List<@NotNull Long> itemSns) {
}
