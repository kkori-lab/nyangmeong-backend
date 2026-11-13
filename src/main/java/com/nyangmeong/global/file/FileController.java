package com.nyangmeong.global.file;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [공통 파일 컨트롤러]
 * - GET /api/files/{atchFileSn} : 로그인 회원 누구나 여는 파일 (프로필·게시글 사진·캐릭터·착용 이미지·아이템·공지·뉴스)
 * - GET /api/files/public/{atchFileSn} : 예전 주소. 프론트가 옮길 때까지 같은 동작(로그인 필요)
 * - 도메인 API 로만 주는 파일(영수증·채팅·문의·검진 등)은 번호를 알아도 404 : 각 도메인 API 가 소유 검사 후 준다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStore fileStore;

    @Operation(summary = "로그인 회원 누구나 여는 파일")
    @GetMapping("/{atchFileSn}")
    public ResponseEntity<byte[]> getShared(@PathVariable Long atchFileSn) {
        return toResponse(fileStore.loadShared(atchFileSn));
    }

    @Operation(summary = "로그인 회원 누구나 여는 파일 (예전 주소, /api/files/{atchFileSn} 로 옮긴다)")
    @GetMapping("/public/{atchFileSn}")
    public ResponseEntity<byte[]> getPublic(@PathVariable Long atchFileSn) {
        return toResponse(fileStore.loadShared(atchFileSn));
    }

    private ResponseEntity<byte[]> toResponse(StoredFile file) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
