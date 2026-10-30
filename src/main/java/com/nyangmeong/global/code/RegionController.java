package com.nyangmeong.global.code;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [지역 컨트롤러]
 * - 시도 → 시군구 → 읍면동 연동 콤보박스 (FN-CMU-002-03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "하위 지역 목록(없으면 시도)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RegionSummary>>> search(@RequestParam(required = false) Long upRgnSn) {
        return ResponseEntity.ok(ApiResponse.success(regionService.getRegions(upRgnSn)));
    }
}
