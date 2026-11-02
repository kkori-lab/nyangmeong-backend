package com.nyangmeong.pet.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.domain.CharacterImagePart;
import com.nyangmeong.pet.service.PixelCharacterService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [반려동물 캐릭터 컨트롤러]
 * - /pets/{petSn} 아래 캐릭터 이미지 (SCR-MYP-010, MYP-001·002 얼굴 원형)
 * - 이미지는 파일 자체 응답 (docs/convention/api.md 보호 파일 다운로드)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}")
public class PetCharacterController {

    private final PixelCharacterService pixelCharacterService;

    @Operation(summary = "캐릭터 이미지(part=full 전신, face 얼굴)")
    @GetMapping("/character-image")
    public ResponseEntity<byte[]> getCharacterImage(@AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @RequestParam(defaultValue = "full") String part) {
        CharacterImagePart imagePart = CharacterImagePart.of(part)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
        StoredFile image = pixelCharacterService.getCharacterImage(me.id(), petSn, imagePart);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
