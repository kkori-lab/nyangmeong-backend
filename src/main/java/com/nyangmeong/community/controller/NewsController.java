package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.NewsResponse;
import com.nyangmeong.community.dto.NewsSearchCond;
import com.nyangmeong.community.dto.NewsSummary;
import com.nyangmeong.community.service.NewsService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [뉴스 컨트롤러]
 * - SCR-CMU-003 목록·상세, 상세 하단 최신 5건은 목록을 size=5 로 호출
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "펫뉴스 목록 (카테고리·제목/내용 검색, 최신순)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NewsSummary>>> search(NewsSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(newsService.search(cond, pq)));
    }

    @Operation(summary = "펫뉴스 상세")
    @GetMapping("/{newsSn}")
    public ResponseEntity<ApiResponse<NewsResponse>> getNews(@PathVariable Long newsSn) {
        return ResponseEntity.ok(ApiResponse.success(newsService.getNews(newsSn)));
    }
}
