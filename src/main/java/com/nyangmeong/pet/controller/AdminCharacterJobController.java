package com.nyangmeong.pet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.pet.dto.AdminCharacterJobListResponse;
import com.nyangmeong.pet.dto.AdminCharacterJobSearchCond;
import com.nyangmeong.pet.service.AdminCharacterJobService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 캐릭터 생성 현황 컨트롤러]
 * - SCR-ADM-082 처리 현황 캐릭터 생성 탭 (FN-ADM-082-02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/processing/character-jobs")
public class AdminCharacterJobController {

    private final AdminCharacterJobService adminCharacterJobService;

    @Operation(summary = "캐릭터 생성 현황(상태별 건수·목록)")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminCharacterJobListResponse>> search(AdminCharacterJobSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminCharacterJobService.search(cond, pq)));
    }
}
