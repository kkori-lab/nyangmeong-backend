package com.nyangmeong.community.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [채팅 메시지 전송 요청]
 * - multipart/form-data, 텍스트(msgCn)·사진(images 최대 10장)·주소 공유(shrnAddr + 선택 dtlAddr)
 * - 유형별 필수 항목은 서비스가 검사한다
 */
public record ChatMessageCreateRequest(
        @NotBlank String msgTypeCd,
        @Size(max = 2000) String msgCn,
        @Size(max = 200) String shrnAddr,
        @Size(max = 30) String dtlAddr,
        List<MultipartFile> images,
        @NotBlank @Size(max = 100) String idmptDmndId) {
}
