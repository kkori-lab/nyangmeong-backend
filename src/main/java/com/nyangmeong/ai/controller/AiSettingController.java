package com.nyangmeong.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.AiSettingResponse;
import com.nyangmeong.ai.service.AiSettingService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [AI 설정값 내부 API 컨트롤러]
 * - /internal/ai/settings : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/settings")
@RequiredArgsConstructor
public class AiSettingController {

    private final AiSettingService aiSettingService;

    @Operation(summary = "AI 설정값 조회(응답 토큰 상한·대화 턴 수)")
    @GetMapping
    public ResponseEntity<ApiResponse<AiSettingResponse>> getSetting() {
        return ResponseEntity.ok(ApiResponse.success(aiSettingService.getSetting()));
    }
}
