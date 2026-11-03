package com.nyangmeong.pet.dto;

import java.util.List;

/**
 * [굽기 대상 캐릭터 페이지]
 * - FN-ADM-070-02
 * - last : 마지막 페이지 여부
 */
public record BakeTargetPageResponse(List<Long> pixelCharSns, boolean last) {
}
