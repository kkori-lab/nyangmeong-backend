package com.nyangmeong.health.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.DailyHealthRecordCreateRequest;
import com.nyangmeong.health.dto.DailyHealthRecordResponse;
import com.nyangmeong.health.dto.DailyHealthRecordSearchCond;
import com.nyangmeong.health.dto.DailyHealthRecordSummary;
import com.nyangmeong.health.dto.DailyHealthRecordUpdateRequest;
import com.nyangmeong.health.service.DailyHealthRecordService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [일상 건강기록 컨트롤러]
 * - SCR-HLT-002 일상 건강기록 목록·등록·수정·삭제 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/daily-health-records")
public class DailyHealthRecordController {

    private final DailyHealthRecordService dailyHealthRecordService;

    @Operation(summary = "일상 건강기록 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyHealthRecordSummary>>> search(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @Valid @ModelAttribute DailyHealthRecordSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(dailyHealthRecordService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "일상 건강기록 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyHealthRecordResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody DailyHealthRecordCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(dailyHealthRecordService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "일상 건강기록 수정")
    @PatchMapping("/{dailyHlthRcdSn}")
    public ResponseEntity<ApiResponse<DailyHealthRecordResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long dailyHlthRcdSn,
            @Valid @RequestBody DailyHealthRecordUpdateRequest req) {
        return ResponseEntity
                .ok(ApiResponse.success(dailyHealthRecordService.update(me.id(), petSn, dailyHlthRcdSn, req)));
    }

    @Operation(summary = "일상 건강기록 삭제")
    @DeleteMapping("/{dailyHlthRcdSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long dailyHlthRcdSn) {
        dailyHealthRecordService.delete(me.id(), petSn, dailyHlthRcdSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
