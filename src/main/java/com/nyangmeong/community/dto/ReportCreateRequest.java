package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [게시글·댓글·채팅 상대 신고 요청]
 * - pstSn·cmntSn·otoChatRoomSn 중 하나만, 사유가 기타면 상세 내용 필수 (FN-CMU-001-13)
 * - otoChatRoomSn : 그 방의 상대 회원을 신고 (대상 회원은 서버가 방에서 정함)
 */
public record ReportCreateRequest(
        Long pstSn,
        Long cmntSn,
        Long otoChatRoomSn,
        @NotBlank String dclrRsnCd,
        @Size(max = 500) String dclrDtlCn) {

    /**
     * 게시글·댓글 신고
     */
    public ReportCreateRequest(Long pstSn, Long cmntSn, String dclrRsnCd, String dclrDtlCn) {
        this(pstSn, cmntSn, null, dclrRsnCd, dclrDtlCn);
    }
}
