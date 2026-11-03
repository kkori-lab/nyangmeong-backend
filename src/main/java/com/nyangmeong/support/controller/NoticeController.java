package com.nyangmeong.support.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.support.dto.NoticeResponse;
import com.nyangmeong.support.dto.NoticeSearchCond;
import com.nyangmeong.support.dto.NoticeSummary;
import com.nyangmeong.support.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [공지사항 컨트롤러]
 * - SCR-CSC-001 목록·상세
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 목록 (상단 고정 우선, latestYn=true 면 최신순)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummary>>> search(NoticeSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.search(cond, pq)));
    }

    @Operation(summary = "공지사항 상세")
    @GetMapping("/{ntcMttrSn}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(@PathVariable Long ntcMttrSn) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotice(ntcMttrSn)));
    }
}
