package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 캐릭터 생성 시간 초과 배치]
 */
class CharacterJobTimeoutJobTest {

    private final CharacterCreationService service = mock(CharacterCreationService.class);
    private final CharacterJobTimeoutJob job = new CharacterJobTimeoutJob(service);

    @Test
    void 시간을_넘긴_작업만_시간_초과_실패로_처리한다() {
        when(service.searchTimedOutJobs()).thenReturn(List.of(5L, 6L));

        assertThat(job.execute()).isEqualTo(2);

        verify(service).searchTimedOutJobs();
        verify(service).fail(5L, "CHARACTER_TIMEOUT");
        verify(service).fail(6L, "CHARACTER_TIMEOUT");
        verifyNoMoreInteractions(service);
    }
}
