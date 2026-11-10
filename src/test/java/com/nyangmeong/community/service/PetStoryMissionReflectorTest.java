package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 펫이야기 케어미션 반영]
 * - 스프링 컨텍스트 없이 트랜잭션 매니저와 미션 서비스를 가짜로 대체
 */
class PetStoryMissionReflectorTest {

    private static final Long MBR = 1L;

    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final PetStoryMissionReflector reflector;

    PetStoryMissionReflectorTest() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        reflector = new PetStoryMissionReflector(careMissionService, transactionManager);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션_밖에서는_바로_반영한다() {
        reflector.reflectAfterCommit(MBR, CareMission.PET_STORY_POST);

        verify(careMissionService).reflectRepresentative(MBR, CareMission.PET_STORY_POST);
    }

    @Test
    void 트랜잭션_안에서는_커밋_전에는_반영하지_않고_커밋_뒤에_반영한다() {
        TransactionSynchronizationManager.initSynchronization();

        reflector.reflectAfterCommit(MBR, CareMission.PET_STORY_COMMENT);

        verify(careMissionService, never()).reflectRepresentative(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(careMissionService).reflectRepresentative(MBR, CareMission.PET_STORY_COMMENT);
    }

    @Test
    void 반영이_실패해도_예외는_밖으로_나가지_않는다() {
        doThrow(new IllegalStateException("미션 오류")).when(careMissionService).reflectRepresentative(any(), any());

        assertThatCode(() -> reflector.reflectAfterCommit(MBR, CareMission.PET_STORY_POST))
                .doesNotThrowAnyException();
        verify(transactionManager).rollback(any());
    }
}
