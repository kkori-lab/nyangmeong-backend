package com.nyangmeong.community.dto;

/**
 * [관리자 뉴스 수정 요청]
 * - PATCH /api/admin/news/{newsSn} (FN-ADM-050-02·03), 보낸 칸만 바꿈, 하나도 없으면 400
 * - newsTtl·newsCn·atchFileSn : 관리자 등록 기사만 (수집 기사에 보내면 400)
 * - newsClsfCd·useYn : 수집 기사·관리자 등록 기사 모두, useYn=false 면 사용자 뉴스에서 빠진다
 */
public record AdminNewsUpdateRequest(String newsTtl, String newsCn, String newsClsfCd, Long atchFileSn,
        Boolean useYn) {
}
