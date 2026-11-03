package com.nyangmeong.health.dto;

import java.time.LocalDate;

/**
 * [영수증 검진기록 생성 값]
 * - 영수증 확인 후 저장(expense)이 반려동물마다 넘기는 값 (FN-HLT-002-45)
 * - schdlSn 이 있으면 그 일정을 검진일로 옮겨 연결, 없으면 같은 날 병원 일정에 연결하고 없으면 만든다 (FN-HLT-002-46)
 */
public record ReceiptCheckupCommand(Long recptSn, LocalDate ckupYmd, String hsptlNm, String ckupTypeCd,
        String ckupCn, String ckupMemoCn, Long schdlSn) {
}
