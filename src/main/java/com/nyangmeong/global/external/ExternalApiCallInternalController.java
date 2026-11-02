package com.nyangmeong.global.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [외부 API 호출 통지 내부 API 컨트롤러]
 * - /internal/ai/external-api-calls : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출
 * - 호출 수·실패·토큰 기록 (FN-ADM-112-01·02). Python 은 통지 실패를 사용자 응답에 반영하지 않음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ai/external-api-calls")
public class ExternalApiCallInternalController {

    private final ExternalApiUsageService externalApiUsageService;

    @Operation(summary = "외부 API 호출 통지")
    @PostMapping
    public ResponseEntity<ApiResponse<ExternalApiCallResponse>> record(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ExternalApiCallRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(externalApiUsageService.record(me.id(), req)));
    }
}
