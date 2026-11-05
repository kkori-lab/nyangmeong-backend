package com.nyangmeong.place.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.aitool.AiTool;
import com.nyangmeong.global.aitool.AiToolKind;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.place.dto.PlaceSearchCond;
import com.nyangmeong.place.dto.PlaceSummary;
import com.nyangmeong.place.service.PlaceService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [장소 컨트롤러]
 * - SCR-HOS-001·PLC-001~004 장소 검색, 커뮤니티 지도 선택도 같은 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "장소 검색 (카테고리·키워드·지도 범위, 추천·거리·이름순, 15건씩)")
    @AiTool(name = "list_places", domain = "place", kind = AiToolKind.READ)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlaceSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute PlaceSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(placeService.search(me.id(), cond, pq)));
    }
}
