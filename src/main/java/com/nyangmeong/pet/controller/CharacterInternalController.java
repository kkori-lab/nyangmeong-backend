package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import com.nyangmeong.pet.dto.BakeItemSummary;
import com.nyangmeong.pet.dto.CharacterFailureRequest;
import com.nyangmeong.pet.dto.CharacterJobCreateRequest;
import com.nyangmeong.pet.dto.CharacterJobResponse;
import com.nyangmeong.pet.dto.CharacterSheetsResponse;
import com.nyangmeong.pet.dto.CharacterSuccessRequest;
import com.nyangmeong.pet.dto.CharacterSuccessResponse;
import com.nyangmeong.pet.dto.EquippedImageSaveRequest;
import com.nyangmeong.pet.dto.ItemSheetSaveRequest;
import com.nyangmeong.pet.service.CharacterCreationService;
import com.nyangmeong.pet.service.CharacterSheetService;
import com.nyangmeong.pet.service.PixelCharacterService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [캐릭터 내부 API 컨트롤러]
 * - /internal/ai/characters : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (docs/api/pet.md)
 * - 생성·합성은 Python, Java 는 작업 상태와 결과·시트 저장만 (FN-MYP-010-08·09·12, FN-MYP-011-01)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ai/characters")
public class CharacterInternalController {

    private final PixelCharacterService pixelCharacterService;
    private final CharacterCreationService characterCreationService;
    private final CharacterSheetService characterSheetService;

    @Operation(summary = "캐릭터 생성 작업 생성")
    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<CharacterJobResponse>> createJob(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody CharacterJobCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(pixelCharacterService.createJob(me.id(), req.petSn())));
    }

    @Operation(summary = "캐릭터 생성 성공 보고")
    @PostMapping(value = "/jobs/{charCrtJobSn}/success", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CharacterSuccessResponse>> reportSuccess(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long charCrtJobSn, @Valid @ModelAttribute CharacterSuccessRequest req) {
        return ResponseEntity.ok(ApiResponse.success(characterCreationService.reportSuccess(me.id(), charCrtJobSn, req)));
    }

    @Operation(summary = "캐릭터 생성 실패 보고")
    @PostMapping("/jobs/{charCrtJobSn}/failure")
    public ResponseEntity<ApiResponse<Void>> reportFailure(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long charCrtJobSn, @Valid @RequestBody CharacterFailureRequest req) {
        characterCreationService.reportFailure(me.id(), charCrtJobSn, req.failRsn());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "굽기 대상 아이템 조회")
    @GetMapping("/bake-items")
    public ResponseEntity<ApiResponse<List<BakeItemSummary>>> searchBakeItems() {
        return ResponseEntity.ok(ApiResponse.success(characterSheetService.searchBakeItems()));
    }

    @Operation(summary = "착용 시트 저장")
    @PutMapping(value = "/{pixelCharSn}/item-sheets/{itemSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> saveItemSheet(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @PathVariable Long itemSn, @Valid @ModelAttribute ItemSheetSaveRequest req) {
        characterSheetService.saveItemSheet(me.id(), me.isAdmin(), pixelCharSn, itemSn, req.sheet());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "착용 모습 대표 이미지 저장")
    @PutMapping(value = "/{pixelCharSn}/equipped-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> saveEquippedImage(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @Valid @ModelAttribute EquippedImageSaveRequest req) {
        pixelCharacterService.saveEquippedImage(me.id(), pixelCharSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "굽기 실패 표시")
    @PostMapping("/{pixelCharSn}/item-sheets/{itemSn}/failure")
    public ResponseEntity<ApiResponse<Void>> failItemSheet(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @PathVariable Long itemSn) {
        characterSheetService.failItemSheet(me.id(), me.isAdmin(), pixelCharSn, itemSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "합성용 시트 정보 조회")
    @GetMapping("/{pixelCharSn}/sheets")
    public ResponseEntity<ApiResponse<CharacterSheetsResponse>> getSheets(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @RequestParam(required = false) List<Long> itemSns) {
        return ResponseEntity.ok(ApiResponse.success(characterSheetService.getSheets(me.id(), me.isAdmin(), pixelCharSn, itemSns)));
    }

    @Operation(summary = "시트 파일 내려받기")
    @GetMapping("/{pixelCharSn}/sheet-files/{atchFileSn}")
    public ResponseEntity<byte[]> getSheetFile(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @PathVariable Long atchFileSn) {
        StoredFile file = characterSheetService.getSheetFile(me.id(), me.isAdmin(), pixelCharSn, atchFileSn);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
