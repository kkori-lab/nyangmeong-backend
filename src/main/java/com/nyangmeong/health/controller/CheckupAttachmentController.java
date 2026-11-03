package com.nyangmeong.health.controller;

import java.nio.charset.StandardCharsets;

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
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.CheckupAttachmentCreateRequest;
import com.nyangmeong.health.dto.CheckupAttachmentResponse;
import com.nyangmeong.health.dto.CheckupAttachmentSearchCond;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;
import com.nyangmeong.health.dto.CheckupAttachmentUpdateRequest;
import com.nyangmeong.health.service.CheckupAttachmentService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [검진기록 첨부 컨트롤러]
 * - SCR-HLT-005 첨부 자료 목록·등록·파일·교체·삭제 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/checkup-attachments")
public class CheckupAttachmentController {

    private final CheckupAttachmentService checkupAttachmentService;

    @Operation(summary = "검진기록 첨부 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CheckupAttachmentSummary>>> search(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @Valid @ModelAttribute CheckupAttachmentSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(checkupAttachmentService.search(me.id(), petSn, cond, pq)));
    }

    @Operation(summary = "검진기록 첨부 파일")
    @GetMapping("/{ckupRcdAtchSn}")
    public ResponseEntity<byte[]> getFile(@AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @PathVariable Long ckupRcdAtchSn) {
        StoredFile file = checkupAttachmentService.getFile(me.id(), petSn, ckupRcdAtchSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.orgnlFileNm(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @Operation(summary = "검진기록 첨부 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckupAttachmentResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute CheckupAttachmentCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(checkupAttachmentService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "검진기록 첨부 교체·수정")
    @PatchMapping(value = "/{ckupRcdAtchSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckupAttachmentResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long ckupRcdAtchSn,
            @Valid @ModelAttribute CheckupAttachmentUpdateRequest req) {
        return ResponseEntity
                .ok(ApiResponse.success(checkupAttachmentService.update(me.id(), petSn, ckupRcdAtchSn, req)));
    }

    @Operation(summary = "검진기록 첨부 삭제")
    @DeleteMapping("/{ckupRcdAtchSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long ckupRcdAtchSn) {
        checkupAttachmentService.delete(me.id(), petSn, ckupRcdAtchSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
