package com.nyangmeong.pet.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.BakeTargetPageResponse;
import com.nyangmeong.pet.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [아이템 내부 API 컨트롤러]
 * - /internal/ai/items : Python AI 서버가 관리자 쿠키를 그대로 넘겨 호출 (docs/api/pet.md)
 * - FN-ADM-070-02 전체 굽기 대상 캐릭터
 * - 굽기 참고 그림은 파일 자체 응답, 회원·관리자 쿠키 모두 (FN-MYP-011-01)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ai/items")
public class ItemInternalController {

    private final ItemService itemService;

    @Operation(summary = "굽기 대상 캐릭터 조회")
    @GetMapping("/{itemSn}/bake-targets")
    public ResponseEntity<ApiResponse<BakeTargetPageResponse>> searchBakeTargets(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long itemSn,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.success(itemService.searchBakeTargets(me.isAdmin(), itemSn, page)));
    }

    @Operation(summary = "굽기 참고용 아이템 그림 내려받기")
    @GetMapping("/{itemSn}/bake-image")
    public ResponseEntity<byte[]> getBakeImage(@PathVariable Long itemSn) {
        StoredFile file = itemService.getBakeImage(itemSn);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
