package com.nyangmeong.health.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.ObservationCycleCreateRequest;
import com.nyangmeong.health.dto.ObservationCycleResponse;
import com.nyangmeong.health.dto.ObservationCycleSearchCond;
import com.nyangmeong.health.dto.ObservationCycleSummary;
import com.nyangmeong.health.dto.ObservationCycleUpdateRequest;
import com.nyangmeong.health.dto.ObservationItemCreateRequest;
import com.nyangmeong.health.dto.ObservationItemResponse;
import com.nyangmeong.health.dto.ObservationItemSearchCond;
import com.nyangmeong.health.dto.ObservationItemSummary;
import com.nyangmeong.health.service.ObservationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관찰 컨트롤러]
 * - SCR-HLT-002 관찰 항목 목록·등록·종료, 관찰 회차 목록·사진·추가·수정·삭제 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}")
public class ObservationController {

    private final ObservationService observationService;

    @Operation(summary = "관찰 항목 목록")
    @GetMapping("/observation-items")
    public ResponseEntity<ApiResponse<List<ObservationItemSummary>>> searchItems(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @Valid @ModelAttribute ObservationItemSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(observationService.searchItems(me.id(), petSn, cond)));
    }

    @Operation(summary = "관찰 항목 등록(1회차 포함)")
    @PostMapping(value = "/observation-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObservationItemResponse>> createItem(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute ObservationItemCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(observationService.createItem(me.id(), petSn, req)));
    }

    @Operation(summary = "관찰 종료")
    @PostMapping("/observation-items/{obsrvArtclSn}/end")
    public ResponseEntity<ApiResponse<Void>> endItem(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long obsrvArtclSn) {
        observationService.endItem(me.id(), petSn, obsrvArtclSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "관찰 회차 목록")
    @GetMapping("/observation-cycles")
    public ResponseEntity<ApiResponse<List<ObservationCycleSummary>>> searchCycles(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @Valid @ModelAttribute ObservationCycleSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(observationService.searchCycles(me.id(), petSn, cond)));
    }

    @Operation(summary = "관찰 사진")
    @GetMapping("/observation-cycles/{obsrvCyclSn}/photo")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @PathVariable Long obsrvCyclSn) {
        StoredFile file = observationService.getPhoto(me.id(), petSn, obsrvCyclSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.orgnlFileNm(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @Operation(summary = "관찰 회차 추가")
    @PostMapping(value = "/observation-cycles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObservationCycleResponse>> createCycle(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @Valid @ModelAttribute ObservationCycleCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(observationService.createCycle(me.id(), petSn, req)));
    }

    @Operation(summary = "관찰 회차 수정")
    @PatchMapping(value = "/observation-cycles/{obsrvCyclSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObservationCycleResponse>> updateCycle(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn, @PathVariable Long obsrvCyclSn,
            @Valid @ModelAttribute ObservationCycleUpdateRequest req) {
        return ResponseEntity
                .ok(ApiResponse.success(observationService.updateCycle(me.id(), petSn, obsrvCyclSn, req)));
    }

    @Operation(summary = "관찰 회차 삭제")
    @DeleteMapping("/observation-cycles/{obsrvCyclSn}")
    public ResponseEntity<ApiResponse<Void>> deleteCycle(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long obsrvCyclSn) {
        observationService.deleteCycle(me.id(), petSn, obsrvCyclSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
