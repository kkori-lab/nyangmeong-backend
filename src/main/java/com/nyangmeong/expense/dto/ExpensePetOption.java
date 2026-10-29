package com.nyangmeong.expense.dto;

import lombok.Data;

/**
 * [지출 필터 반려동물]
 * - 지금 연동된 반려동물과 내 지출이 남은 반려동물 (FN-MYP-030-10)
 * - petDelYn 이면 "(삭제됨)", petLinked 가 false 면 "(연동 해제)"
 */
@Data
public class ExpensePetOption {
    private Long petSn;
    private String petNm;
    private Boolean petDelYn;
    private Boolean petLinked;
}
