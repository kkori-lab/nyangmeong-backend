package com.nyangmeong.place.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiTool;
import com.nyangmeong.global.aitool.AiToolKind;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.place.dto.PlaceBookmarkCreateRequest;
import com.nyangmeong.place.dto.PlaceBookmarkResponse;
import com.nyangmeong.place.dto.PlaceBookmarkSummary;
import com.nyangmeong.place.service.PlaceBookmarkService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [장소 즐겨찾기 컨트롤러]
 * - SCR-HOS-001 목록·저장·해제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place-bookmarks")
public class PlaceBookmarkController {

    private final PlaceBookmarkService placeBookmarkService;

    @Operation(summary = "내 장소 즐겨찾기 목록 (카테고리별, 저장 최신순)")
    @AiTool(name = "list_place_bookmarks", domain = "place", kind = AiToolKind.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceBookmarkSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @AiCode("PLCG01") @RequestParam String plcClsfCd) {
        return ResponseEntity.ok(ApiResponse.success(placeBookmarkService.search(me.id(), plcClsfCd)));
    }

    @Operation(summary = "장소 즐겨찾기 저장")
    @PostMapping
    public ResponseEntity<ApiResponse<PlaceBookmarkResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody PlaceBookmarkCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(placeBookmarkService.create(me.id(), req)));
    }

    @Operation(summary = "장소 즐겨찾기 해제")
    @DeleteMapping("/{plcBkmkSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long plcBkmkSn) {
        placeBookmarkService.delete(me.id(), plcBkmkSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
