package com.nyangmeong.pet.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.pet.domain.PetCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [반려동물 등록 직후 케어미션 출제]
 * - PetCreatedEvent 를 등록 트랜잭션이 커밋된 뒤 받아 그날 미션을 바로 출제 (SCR-MYP-003)
 * - 새 트랜잭션(REQUIRES_NEW)에서 출제하고 예외는 로그만 남김 : 정각 배치와 겹쳐 UK_DAY_MSN_1 에 걸려도 등록은 성공, 못 채운 미션은 다음 정각 배치가 채움
 * - 새 반려동물은 관찰 항목이 없으므로 관찰 중 여부는 false (health 는 윗층이라 부르지 않음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetCreatedEventListener {

    private final CareMissionService careMissionService;

    /**
     * 등록한 반려동물의 오늘 미션 출제
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPetCreated(PetCreatedEvent event) {
        try {
            careMissionService.issueFor(event.petSn(), event.msnCrtrYmd(), false);
        } catch (RuntimeException e) {
            log.warn("등록 직후 케어미션 출제 실패, 다음 정각 배치가 채움 petSn={}", event.petSn(), e);
        }
    }
}
