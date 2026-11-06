package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [비용 수령 확인]
 * - rcvYn false 면 돌봄 완료 상태를 유지한다
 */
public record CareMatchReceiptConfirmRequest(@NotNull Boolean rcvYn) {
}
