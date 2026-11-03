package com.nyangmeong.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 공지 등록 요청]
 * - POST /api/admin/notices (FN-ADM-040-02·03·05)
 * - ntcMttrCn : 이미지는 ![](/api/files/public/{atchFileSn}) 한 줄로 넣는다
 * - notiSndngYn : 등록 때만 받는다. 수정 때는 다시 보내지 않음
 */
public record AdminNoticeCreateRequest(
        @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.") String ntcMttrTtl,
        @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 4000, message = "내용은 4000자 이내로 입력해 주세요.") String ntcMttrCn,
        @NotNull(message = "상단 고정 여부를 선택해 주세요.") Boolean upendFixYn,
        @NotNull(message = "사용 여부를 선택해 주세요.") Boolean useYn,
        @NotNull(message = "알림 발송 여부를 선택해 주세요.") Boolean notiSndngYn) {
}
