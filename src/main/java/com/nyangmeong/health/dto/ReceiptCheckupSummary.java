package com.nyangmeong.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [영수증으로 만든 검진기록]
 * - 같은 요청 재전송 응답용 (FN-HLT-002-45)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptCheckupSummary {

    /** 반려동물일련번호 */
    private Long petSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
}
