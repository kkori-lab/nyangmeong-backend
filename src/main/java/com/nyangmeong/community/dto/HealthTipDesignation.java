package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [건강팁 지정 자리]
 * - FN-ADM-023-01 : 지정 글 번호와 노출 자리(1~3)
 */
@Data
public class HealthTipDesignation {
    private Long pstSn;
    private Integer hlthInfoExpsrSeq;
}
