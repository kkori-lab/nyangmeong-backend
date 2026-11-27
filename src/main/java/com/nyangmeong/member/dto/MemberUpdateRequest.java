package com.nyangmeong.member.dto;

import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.aitool.AiExclude;

/**
 * [회원정보 수정 요청]
 * - PATCH /api/members/me, multipart/form-data, 보낸 필드만 수정 (FN-MYP-040-02·05·08·11)
 * - mblTelno 는 정규화·형식·중복 검사를 서비스에서 한다(회원가입과 같은 기준)
 * - mblTelno 는 AI 도구 목록에 싣지 않는다 : 휴대전화번호를 외부 AI에 보내지 않는다
 * - profilePhotoReset 이 true 면 기본 이미지로 되돌린다, profilePhoto 와 함께 보내면 400
 */
public record MemberUpdateRequest(
        @AiExclude String mblTelno,
        MultipartFile profilePhoto,
        Boolean profilePhotoReset) {
}
