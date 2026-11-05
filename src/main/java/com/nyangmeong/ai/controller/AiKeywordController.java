package com.nyangmeong.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.AiKeywordResponse;
import com.nyangmeong.ai.service.AiKeywordService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [AI 키워드 사전 내부 API 컨트롤러]
 * - /internal/ai/keywords : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (FN-ADM-081, docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/keywords")
@RequiredArgsConstructor
public class AiKeywordController {

    private final AiKeywordService aiKeywordService;

    @Operation(summary = "키워드 사전 조회(사용 중 행, 정렬순서 순)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AiKeywordResponse>>> getKeywords(@RequestParam String dictUsgCd) {
        return ResponseEntity.ok(ApiResponse.success(aiKeywordService.getActiveKeywords(dictUsgCd)));
    }
}
