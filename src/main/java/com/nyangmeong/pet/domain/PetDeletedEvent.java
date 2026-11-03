package com.nyangmeong.pet.domain;

/**
 * [반려동물 삭제 이벤트]
 * - PetService.deletePet 이 같은 트랜잭션 안에서 발행한다. 위층(health·ai)이 받아 그 반려동물의 자기 기록을 DEL_YN = 'Y' 로 바꾼다
 * - 받는 쪽은 같은 트랜잭션에서 처리한다 : @TransactionalEventListener(phase = BEFORE_COMMIT) 또는 @EventListener
 *   AFTER_COMMIT 으로 받으면 처리가 실패해도 반려동물 삭제는 이미 커밋되어 기록이 남는다
 * - 가계부(expense)는 받지 않는다 : 지출은 유지하고 "(삭제됨)"으로 표시 (FN-MYP-002-07)
 */
public record PetDeletedEvent(Long petSn) {
}
