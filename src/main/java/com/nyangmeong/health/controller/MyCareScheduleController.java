package com.nyangmeong.health.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.aitool.AiTool;
import com.nyangmeong.global.aitool.AiToolKind;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.dto.MyCareScheduleSearchCond;
import com.nyangmeong.health.service.CareScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [회원 단위 케어 캘린더 일정 컨트롤러]
 * - 여러 반려동물 일정을 한 번에 조회 : 케어 캘린더 화면당 호출 수 고정 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/care-schedules")
public class MyCareScheduleController {

    private final CareScheduleService careScheduleService;

    @Operation(summary = "케어 캘린더 일정 목록(여러 반려동물)")
    @AiTool(name = "list_my_care_schedules", domain = "health", kind = AiToolKind.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CareScheduleSummary>>> searchMine(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute MyCareScheduleSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(careScheduleService.searchMine(me.id(), cond)));
    }
}
