package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.dto.AdminCharacterJobStatusCount;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 관리자 대시보드 캐릭터 생성 실패 수]
 */
class AdminCharacterJobFailCountTest {

    private final PixelCharacterMapper mapper = mock(PixelCharacterMapper.class);
    private final AdminCharacterJobService service = new AdminCharacterJobService(mapper);

    @Test
    void 생성_실패_상태_건수만_돌려준다() {
        when(mapper.selectAdminJobStatusCounts()).thenReturn(List.of(
                new AdminCharacterJobStatusCount(CharacterJobStatus.FAILED.getCode(), 3L),
                new AdminCharacterJobStatusCount(CharacterJobStatus.REQUESTED.getCode(), 9L)));

        assertThat(service.countFailed()).isEqualTo(3);
    }

    @Test
    void 실패가_없으면_0이다() {
        when(mapper.selectAdminJobStatusCounts()).thenReturn(List.of());

        assertThat(service.countFailed()).isZero();
    }
}
