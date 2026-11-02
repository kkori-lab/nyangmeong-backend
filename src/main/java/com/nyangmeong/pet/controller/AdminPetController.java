package com.nyangmeong.pet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.dto.AdminPetSummary;
import com.nyangmeong.pet.service.PetService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 반려동물 컨트롤러]
 * - SCR-ADM-011 (FN-ADM-011-02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members/{mbrSn}/pets")
public class AdminPetController {

    private final PetService petService;

    @Operation(summary = "회원 반려동물 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPetSummary>>> searchForAdmin(@PathVariable Long mbrSn,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(petService.searchForAdmin(mbrSn, pq)));
    }
}
