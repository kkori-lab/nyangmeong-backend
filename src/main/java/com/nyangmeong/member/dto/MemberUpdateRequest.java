package com.nyangmeong.member.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * [회원정보 수정 요청]
 * - PATCH /api/members/me, multipart/form-data, 보낸 필드만 수정 (FN-MYP-040-02·05·08·11)
 * - mblTelno 는 정규화·형식·중복 검사를 서비스에서 한다(회원가입과 같은 기준)
 * - profilePhotoReset 이 true 면 기본 이미지로 되돌린다, profilePhoto 와 함께 보내면 400
 */
public record MemberUpdateRequest(
        String mblTelno,
        MultipartFile profilePhoto,
        Boolean profilePhotoReset) {
}
