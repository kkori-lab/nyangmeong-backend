package com.nyangmeong.community.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

import lombok.extern.slf4j.Slf4j;

/**
 * [펫이야기 글·댓글의 케어미션 반영 (FN-CMU-001-11·14)]
 * - 글·댓글 저장이 커밋된 뒤에 반영 : 저장이 취소되면 미션도 오르지 않게
 * - 반영은 새 트랜잭션 : 커밋 뒤에는 기존 트랜잭션에 참여해도 쓴 내용이 저장되지 않음
 * - 반영이 실패해도 글·댓글은 그대로 저장되고 경고 로그만 남김 (사람 결정 2026-11-10)
 */
@Slf4j
@Component
public class PetStoryMissionReflector {

    private final CareMissionService careMissionService;
    private final TransactionTemplate newTransaction;

    public PetStoryMissionReflector(CareMissionService careMissionService,
            PlatformTransactionManager transactionManager) {
        this.careMissionService = careMissionService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 커밋 뒤 반영 (트랜잭션 밖에서 부르면 바로 반영)
     */
    public void reflectAfterCommit(Long mbrSn, CareMission mission) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            reflect(mbrSn, mission);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reflect(mbrSn, mission);
            }
        });
    }

    private void reflect(Long mbrSn, CareMission mission) {
        try {
            newTransaction.executeWithoutResult(status -> careMissionService.reflectRepresentative(mbrSn, mission));
        } catch (RuntimeException e) {
            log.warn("펫이야기 케어미션 반영 실패 mbrSn={}, mission={}", mbrSn, mission, e);
        }
    }
}
