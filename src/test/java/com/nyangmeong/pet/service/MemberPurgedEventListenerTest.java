package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.nyangmeong.member.domain.MemberPurgedEvent;

/**
 * [검사 - 회원 파기 때 반려동물 일괄 삭제 리스너]
 */
class MemberPurgedEventListenerTest {

    private static final Long MEMBER = 7L;

    private final PetService petService = mock(PetService.class);
    private final MemberPurgedEventListener listener = new MemberPurgedEventListener(petService);

    @Test
    void 파기_이벤트를_받으면_그_회원의_반려동물을_일괄_처리한다() {
        listener.onMemberPurged(new MemberPurgedEvent(MEMBER));

        verify(petService).purgeMemberPets(MEMBER);
    }

    @Test
    void 일괄_처리에서_예외가_나면_리스너_밖으로_전파된다() {
        doThrow(new IllegalStateException("삭제 실패")).when(petService).purgeMemberPets(MEMBER);

        assertThatThrownBy(() -> listener.onMemberPurged(new MemberPurgedEvent(MEMBER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 이벤트를_발행하면_등록된_리스너가_받아_일괄_처리하고_예외는_발행한_쪽까지_올라간다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PetService.class, () -> petService);
            context.registerBean(MemberPurgedEventListener.class);
            context.refresh();

            context.publishEvent(new MemberPurgedEvent(MEMBER));
            verify(petService).purgeMemberPets(MEMBER);

            doThrow(new IllegalStateException("삭제 실패")).when(petService).purgeMemberPets(MEMBER);
            assertThatThrownBy(() -> context.publishEvent(new MemberPurgedEvent(MEMBER)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
