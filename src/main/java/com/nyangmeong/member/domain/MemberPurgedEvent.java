package com.nyangmeong.member.domain;

/**
 * [회원 파기 이벤트]
 * - MemberPurgeService.purge 가 파기에 성공한 같은 트랜잭션 안에서 발행한다. 위층(pet 등)이 받아 그 회원의 자기 기록을 정리한다
 * - 받는 쪽은 같은 트랜잭션에서 처리한다 : @EventListener 또는 @TransactionalEventListener(phase = BEFORE_COMMIT)
 *   AFTER_COMMIT 으로 받으면 처리가 실패해도 회원 파기는 이미 커밋되어 기록이 남는다
 */
public record MemberPurgedEvent(Long mbrSn) {
}
