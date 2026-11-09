package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.PetShortcutSaveRequest;
import com.nyangmeong.pet.dto.PetShortcutSummary;
import com.nyangmeong.pet.service.PetShortcutService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [반려동물 메인 바로가기 컨트롤러]
 * - 로그인 회원 단위 조회·전체 저장 (FN-MAN-008)
 * - 저장 본문이 배열이라 형식 검사는 서비스 : @Valid 없음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/main-shortcuts")
public class PetShortcutController {

    private final PetShortcutService petShortcutService;

    @Operation(summary = "메인 반려동물 바로가기 조회(저장한 적 없는 반려동물은 종합 건강기록)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetShortcutSummary>>> search(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(petShortcutService.search(me.id())));
    }

    @Operation(summary = "메인 반려동물 바로가기 저장(반려동물마다 보일 순서대로, 본문에 없는 반려동물은 그대로)")
    @PutMapping
    public ResponseEntity<ApiResponse<List<PetShortcutSummary>>> save(@AuthenticationPrincipal LoginMember me,
            @RequestBody List<PetShortcutSaveRequest> req) {
        return ResponseEntity.ok(ApiResponse.success(petShortcutService.save(me.id(), req)));
    }
}
