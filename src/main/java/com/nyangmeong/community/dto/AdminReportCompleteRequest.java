package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 신고 처리 완료 요청]
 * - suspnDayCnt·suspnRsn : 조치가 작성자 정지일 때만, suspnDayCnt 가 null 이면 영구 정지
 */
public record AdminReportCompleteRequest(
        @NotBlank(message = "조치를 선택해 주세요.") String dclrActnCd,
        @Size(max = 1000, message = "처리 메모는 1000자 이내로 입력해 주세요.") String prcsMemo,
        Integer suspnDayCnt,
        @Size(max = 1000, message = "정지 사유는 1000자 이내로 입력해 주세요.") String suspnRsn) {
}
