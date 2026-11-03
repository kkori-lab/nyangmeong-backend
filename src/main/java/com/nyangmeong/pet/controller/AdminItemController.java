package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.AdminItemCreateRequest;
import com.nyangmeong.pet.dto.AdminItemCreateResponse;
import com.nyangmeong.pet.dto.AdminItemOrderRequest;
import com.nyangmeong.pet.dto.AdminItemSummary;
import com.nyangmeong.pet.dto.AdminItemUpdateRequest;
import com.nyangmeong.pet.service.AdminItemService;
import com.nyangmeong.pet.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 아이템 컨트롤러]
 * - SCR-ADM-070 (FN-ADM-070-01~04)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final AdminItemService adminItemService;
    private final ItemService itemService;

    @Operation(summary = "관리자 아이템 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminItemSummary>>> search(
            @RequestParam(required = false) String itemRegnCd) {
        return ResponseEntity.ok(ApiResponse.success(adminItemService.search(itemRegnCd)));
    }

    @Operation(summary = "관리자 아이템 이미지")
    @GetMapping("/{itemSn}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long itemSn) {
        StoredFile file = itemService.getImage(itemSn);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @Operation(summary = "아이템 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminItemCreateResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute AdminItemCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminItemService.create(me.id(), req)));
    }

    @Operation(summary = "아이템 수정·판매 중지")
    @PatchMapping(value = "/{itemSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long itemSn, @Valid @ModelAttribute AdminItemUpdateRequest req) {
        adminItemService.update(me.id(), itemSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "부위 안 아이템 순서 저장")
    @PutMapping("/order")
    public ResponseEntity<ApiResponse<Void>> saveOrder(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminItemOrderRequest req) {
        adminItemService.saveOrder(me.id(), req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
