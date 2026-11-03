package com.nyangmeong.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.ai.service.AiRegisterContextService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [AI 등록 카드 컨텍스트 내부 API 컨트롤러]
 * - /internal/ai/register-context : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/register-context")
@RequiredArgsConstructor
public class AiRegisterContextController {

    private final AiRegisterContextService aiRegisterContextService;

    @Operation(summary = "AI 등록 카드 컨텍스트 조회(반려동물·케어 항목 이름)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RegisterContextPet>>> search(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(aiRegisterContextService.search(me.id())));
    }
}
