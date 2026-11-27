package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiTool;
import com.nyangmeong.global.aitool.AiToolKind;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.pet.dto.BreedSummary;
import com.nyangmeong.pet.service.BreedService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [품종 컨트롤러]
 * - 반려동물 구분별 품종 마스터 (FN-CMU-002-13)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/breeds")
public class BreedController {

    private final BreedService breedService;

    @Operation(summary = "구분별 품종 목록")
    @AiTool(name = "list_breeds", domain = "pet", kind = AiToolKind.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<BreedSummary>>> search(
            @AiCode("PETG01") @RequestParam String petSeCd) {
        return ResponseEntity.ok(ApiResponse.success(breedService.getBreeds(petSeCd)));
    }
}
