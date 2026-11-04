package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.AdminBreedCreateRequest;
import com.nyangmeong.pet.dto.AdminBreedSummary;
import com.nyangmeong.pet.dto.AdminBreedUpdateRequest;
import com.nyangmeong.pet.service.AdminBreedService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 품종 컨트롤러]
 * - SCR-ADM-101 (FN-ADM-101-01)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/breeds")
public class AdminBreedController {

    private final AdminBreedService adminBreedService;

    @Operation(summary = "관리자 품종 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBreedSummary>>> search(
            @RequestParam(required = false) String petSeCd) {
        return ResponseEntity.ok(ApiResponse.success(adminBreedService.search(petSeCd)));
    }

    @Operation(summary = "품종 추가")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminBreedSummary>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminBreedCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminBreedService.create(me.id(), req)));
    }

    @Operation(summary = "품종 수정")
    @PatchMapping("/{vrtySn}")
    public ResponseEntity<ApiResponse<AdminBreedSummary>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long vrtySn, @Valid @RequestBody AdminBreedUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminBreedService.update(me.id(), vrtySn, req)));
    }
}
