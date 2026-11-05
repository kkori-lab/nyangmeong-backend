package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.pet.dto.AdminCharacterJobListResponse;
import com.nyangmeong.pet.dto.AdminCharacterJobSearchCond;
import com.nyangmeong.pet.dto.AdminCharacterJobStatusCount;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 관리자 캐릭터 생성 현황]
 */
class AdminCharacterJobServiceTest {

    private final PixelCharacterMapper pixelCharacterMapper = mock(PixelCharacterMapper.class);
    private final AdminCharacterJobService service = new AdminCharacterJobService(pixelCharacterMapper);

    @Test
    void 상태별_건수는_네_상태를_순서대로_주고_없는_상태는_0() {
        when(pixelCharacterMapper.selectAdminJobStatusCounts()).thenReturn(List.of(
                new AdminCharacterJobStatusCount("CHRC0004", 3), new AdminCharacterJobStatusCount("CHRC0003", 7)));
        when(pixelCharacterMapper.selectAdminJobList(any())).thenReturn(List.of());

        AdminCharacterJobListResponse res = service.search(new AdminCharacterJobSearchCond(),
                new PageQuery(null, null, null));

        assertThat(res.sttsCnts()).extracting(AdminCharacterJobStatusCount::getCrtJobSttsCd)
                .containsExactly("CHRC0001", "CHRC0002", "CHRC0003", "CHRC0004");
        assertThat(res.sttsCnts()).extracting(AdminCharacterJobStatusCount::getCnt).containsExactly(0L, 0L, 7L,
                3L);
        assertThat(res.content()).isEmpty();
    }

    @Test
    void 빈_상태값은_전체로_본다() {
        AdminCharacterJobSearchCond cond = new AdminCharacterJobSearchCond();
        cond.setCrtJobSttsCd(" ");
        when(pixelCharacterMapper.selectAdminJobStatusCounts()).thenReturn(List.of());
        when(pixelCharacterMapper.selectAdminJobList(any())).thenReturn(List.of());

        service.search(cond, new PageQuery(null, null, null));

        assertThat(cond.getCrtJobSttsCd()).isNull();
        verify(pixelCharacterMapper).selectAdminJobList(cond);
    }

    @Test
    void 모르는_상태_코드는_400() {
        AdminCharacterJobSearchCond cond = new AdminCharacterJobSearchCond();
        cond.setCrtJobSttsCd("TMP00044");

        assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(pixelCharacterMapper, never()).selectAdminJobList(any());
    }
}
