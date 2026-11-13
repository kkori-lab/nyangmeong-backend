package com.nyangmeong.community.dto;

/**
 * [관리자 뉴스 썸네일 업로드 응답]
 * - POST /api/admin/news/images (FN-ADM-050-02)
 * - 등록·수정 요청의 atchFileSn 에 넣는다. 로그인 회원 누구나 /api/files/{atchFileSn} 으로 연다
 */
public record AdminNewsImageResponse(Long atchFileSn) {
}
