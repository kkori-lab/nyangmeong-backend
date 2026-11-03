package com.nyangmeong.community.controller;

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

import com.nyangmeong.community.dto.CareRecruitmentCreateRequest;
import com.nyangmeong.community.dto.CareRecruitmentResponse;
import com.nyangmeong.community.dto.CareRecruitmentSearchCond;
import com.nyangmeong.community.dto.CareRecruitmentSummary;
import com.nyangmeong.community.dto.CareRecruitmentUpdateRequest;
import com.nyangmeong.community.service.CareRecruitmentService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [우리동네 펫돌봄 컨트롤러]
 * - SCR-CMU-004 목록·상세·등록·수정·삭제, 등록·수정은 multipart/form-data
 * - 채팅하기는 /api/chat-rooms, 좋아요·신고는 /api/posts, /api/reports 를 같이 쓴다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/care-recruitments")
public class CareRecruitmentController {

    private final CareRecruitmentService careRecruitmentService;

    @Operation(summary = "펫돌봄 모집 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CareRecruitmentSummary>>> search(
            @AuthenticationPrincipal LoginMember me, CareRecruitmentSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(careRecruitmentService.search(me.id(), cond, pq)));
    }

    @Operation(summary = "펫돌봄 모집 상세")
    @GetMapping("/{petPrtcRcrtSn}")
    public ResponseEntity<ApiResponse<CareRecruitmentResponse>> getRecruitment(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petPrtcRcrtSn) {
        return ResponseEntity.ok(ApiResponse.success(careRecruitmentService.getRecruitment(me.id(), petPrtcRcrtSn)));
    }

    @Operation(summary = "펫돌봄 모집 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CareRecruitmentResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute CareRecruitmentCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(careRecruitmentService.create(me.id(), req)));
    }

    @Operation(summary = "펫돌봄 모집 수정(작성자, 모집중만)")
    @PatchMapping(value = "/{petPrtcRcrtSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CareRecruitmentResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcRcrtSn, @Valid @ModelAttribute CareRecruitmentUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(careRecruitmentService.update(me.id(), petPrtcRcrtSn, req)));
    }

    @Operation(summary = "펫돌봄 모집 삭제(작성자)")
    @DeleteMapping("/{petPrtcRcrtSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcRcrtSn) {
        careRecruitmentService.delete(me.id(), petPrtcRcrtSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
