package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.health.domain.ObservationItem;
import com.nyangmeong.health.dto.ObservationCycleCreateRequest;
import com.nyangmeong.health.mapper.ObservationMapper;
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 종료된 관찰 항목에 회차 추가]
 * - AI 도구가 다시 물을 칸을 정하도록 400에 항목 번호 칸 이름을 담는다
 */
class ObservationEndedItemFieldTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long ITEM = 500L;
    private static final String ENDED = "HLTC0056";

    private final ObservationMapper mapper = mock(ObservationMapper.class);
    private final ObservationService service = new ObservationService(mapper, mock(PetAuthorizer.class),
            mock(FileStore.class), mock(CareMissionService.class), mock(PetService.class));

    @Test
    void 종료된_항목에_회차를_추가하면_항목_번호_칸을_담는다() {
        ObservationItem item = new ObservationItem();
        item.setObsrvArtclSn(ITEM);
        item.setPetSn(PET);
        item.setObsrvSttsCd(ENDED);
        when(mapper.selectItem(PET, ITEM)).thenReturn(item);

        assertThatThrownBy(() -> service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM,
                new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1 }),
                LocalDateTime.of(2026, 10, 1, 9, 0), null, null, null)))
                .isInstanceOfSatisfying(CustomException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OBSERVATION_ITEM_ENDED);
                    assertThat(e.getFields()).containsExactly("obsrvArtclSn");
                });
    }
}
