package com.nyangmeong.support.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.support.dto.FaqSearchCond;
import com.nyangmeong.support.dto.FaqSummary;
import com.nyangmeong.support.service.FaqService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [자주 묻는 질문 컨트롤러]
 * - SCR-CSC-002 목록
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService faqService;

    @Operation(summary = "FAQ 목록 (카테고리·질문/답변 검색, 답변 포함)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FaqSummary>>> search(FaqSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(faqService.search(cond, pq)));
    }
}
