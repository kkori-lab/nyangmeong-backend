package com.nyangmeong.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.AiRouteResponse;
import com.nyangmeong.ai.service.AiRouteService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [AI 라우팅 매핑표 내부 API 컨트롤러]
 * - /internal/ai/routes : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (FN-ADM-080-04, docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/routes")
@RequiredArgsConstructor
public class AiRouteController {

    private final AiRouteService aiRouteService;

    @Operation(summary = "라우팅 매핑표 조회(사용 중 행, 우선순위 순)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AiRouteResponse>>> getRoutes() {
        return ResponseEntity.ok(ApiResponse.success(aiRouteService.getActiveRoutes()));
    }
}
