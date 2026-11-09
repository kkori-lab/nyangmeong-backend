package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.pet.domain.PetCreatedEvent;

/**
 * [검사 - 반려동물 등록 직후 케어미션 출제 리스너]
 */
class PetCreatedEventListenerTest {

    private static final Long PET = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final PetCreatedEventListener listener = new PetCreatedEventListener(careMissionService);

    @Test
    void 등록_이벤트를_받으면_관찰_중이_아닌_것으로_그날_미션을_낸다() {
        listener.onPetCreated(new PetCreatedEvent(PET, TODAY));

        verify(careMissionService).issueFor(PET, TODAY, false);
    }

    @Test
    void 출제에서_예외가_나도_리스너는_던지지_않는다() {
        doThrow(new IllegalStateException("UK_DAY_MSN_1 중복")).when(careMissionService).issueFor(PET, TODAY, false);

        assertThatCode(() -> listener.onPetCreated(new PetCreatedEvent(PET, TODAY))).doesNotThrowAnyException();
    }

    @Test
    void 등록이_커밋된_뒤_새_트랜잭션에서_받는다() throws Exception {
        var method = PetCreatedEventListener.class.getMethod("onPetCreated", PetCreatedEvent.class);

        TransactionalEventListener listening = method.getAnnotation(TransactionalEventListener.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(listening).isNotNull();
        assertThat(listening.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
