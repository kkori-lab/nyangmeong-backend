package com.nyangmeong.global.aitool;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [AI 매니저 도구 목록 내부 API 컨트롤러]
 * - /internal/ai/tools : Python AI 서버가 기동 때 쿠키 없이 호출 (docs/api/ai.md)
 * - 설계 : nyangmeong-ai docs/design/tool-catalog.md 3절
 */
@RestController
@RequestMapping("/internal/ai/tools")
@RequiredArgsConstructor
public class AiToolCatalogController {

    private final AiToolCatalogService aiToolCatalogService;

    @Operation(summary = "AI 매니저 도구 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<AiToolCatalogResponse>> getCatalog() {
        return ResponseEntity.ok(ApiResponse.success(aiToolCatalogService.getCatalog()));
    }
}
