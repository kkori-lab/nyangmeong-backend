package com.nyangmeong.pet.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.nyangmeong.member.domain.MemberPurgedEvent;

import lombok.RequiredArgsConstructor;

/**
 * [회원 파기 때 반려동물 일괄 삭제]
 * - MemberPurgedEvent 를 파기 트랜잭션 안에서 바로 받아 그 회원의 반려동물을 정리 (SCR-MYP-002)
 * - 예외를 삼키지 않음 : 반려동물 삭제가 실패하면 회원 파기까지 롤백되고 파기 배치가 다음 주기에 다시 시도
 * - AFTER_COMMIT 이나 별도 트랜잭션으로 받지 않음 : 실패해도 회원 파기만 커밋되어 반려동물이 남음
 */
@Component
@RequiredArgsConstructor
public class MemberPurgedEventListener {

    private final PetService petService;

    /**
     * 파기된 회원의 반려동물 일괄 삭제·공동양육 연동 해제
     */
    @EventListener
    public void onMemberPurged(MemberPurgedEvent event) {
        petService.purgeMemberPets(event.mbrSn());
    }
}
