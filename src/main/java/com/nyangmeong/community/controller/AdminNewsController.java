package com.nyangmeong.community.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.community.dto.AdminNewsCreateRequest;
import com.nyangmeong.community.dto.AdminNewsCreateResponse;
import com.nyangmeong.community.dto.AdminNewsImageResponse;
import com.nyangmeong.community.dto.AdminNewsKeywordCreateRequest;
import com.nyangmeong.community.dto.AdminNewsKeywordCreateResponse;
import com.nyangmeong.community.dto.AdminNewsKeywordResponse;
import com.nyangmeong.community.dto.AdminNewsSearchCond;
import com.nyangmeong.community.dto.AdminNewsSummary;
import com.nyangmeong.community.dto.AdminNewsUpdateRequest;
import com.nyangmeong.community.service.AdminNewsService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 펫뉴스 컨트롤러]
 * - SCR-ADM-050 (FN-ADM-050-01·02·03·05), 뉴스 수집 실행(07)은 POST /api/admin/batches/SYSC0028/run
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final AdminNewsService adminNewsService;

    @Operation(summary = "관리자 뉴스 목록 (숨긴 기사 포함)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminNewsSummary>>> search(AdminNewsSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminNewsService.search(cond, pq)));
    }

    @Operation(summary = "관리자 뉴스 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNewsCreateResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminNewsCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNewsService.create(me.id(), req)));
    }

    @Operation(summary = "뉴스 수정·숨김·카테고리 변경")
    @PatchMapping("/{newsSn}")
    public ResponseEntity<ApiResponse<Void>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long newsSn, @RequestBody AdminNewsUpdateRequest req) {
        adminNewsService.update(me.id(), newsSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "관리자 뉴스 썸네일 업로드")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminNewsImageResponse>> uploadImage(@AuthenticationPrincipal LoginMember me,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNewsService.uploadImage(me.id(), image)));
    }

    @Operation(summary = "뉴스 분류 키워드 목록")
    @GetMapping("/keywords")
    public ResponseEntity<ApiResponse<List<AdminNewsKeywordResponse>>> getKeywords() {
        return ResponseEntity.ok(ApiResponse.success(adminNewsService.getKeywords()));
    }

    @Operation(summary = "뉴스 분류 키워드 등록")
    @PostMapping("/keywords")
    public ResponseEntity<ApiResponse<AdminNewsKeywordCreateResponse>> createKeyword(
            @AuthenticationPrincipal LoginMember me, @Valid @RequestBody AdminNewsKeywordCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNewsService.createKeyword(me.id(), req)));
    }

    @Operation(summary = "뉴스 분류 키워드 삭제")
    @DeleteMapping("/keywords/{kwrdDictSn}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyword(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long kwrdDictSn) {
        adminNewsService.deleteKeyword(me.id(), kwrdDictSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
