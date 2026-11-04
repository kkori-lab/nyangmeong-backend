package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.pet.domain.CareMsn;
import com.nyangmeong.pet.dto.AdminCareMissionSummary;
import com.nyangmeong.pet.dto.AdminCareMissionUpdateRequest;
import com.nyangmeong.pet.mapper.CareMissionMapper;

/**
 * [검사 - 관리자 케어미션 목록·수정]
 */
class AdminCareMissionServiceTest {

    private static final Long ADMIN = 1L;

    private final CareMissionMapper mapper = mock(CareMissionMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminCareMissionService service = new AdminCareMissionService(mapper, adminTaskLogService);

    private CareMsn stored(Long careMsnSn, int cmpnPnt, int achvCrtrNmtm, String useYn) {
        CareMsn careMsn = new CareMsn();
        careMsn.setCareMsnSn(careMsnSn);
        careMsn.setMsnPoolCd("MYPC0010");
        careMsn.setMsnNm("양치하기");
        careMsn.setCmpnPnt(cmpnPnt);
        careMsn.setAchvCrtrNmtm(achvCrtrNmtm);
        careMsn.setMvmnPath("/health/calendar");
        careMsn.setUseYn(useYn);
        return careMsn;
    }

    @Test
    void 풀이_없거나_공백이면_전체_목록이고_사용_여부는_true_false() {
        when(mapper.selectAdminMissions(null)).thenReturn(List.of(stored(1L, 5, 1, "Y"), stored(2L, 5, 1, "N")));

        List<AdminCareMissionSummary> rows = service.search(" ");
        service.search(null);

        assertThat(rows).extracting(AdminCareMissionSummary::useYn).containsExactly(true, false);
        verify(mapper, times(2)).selectAdminMissions(isNull());
    }

    @Test
    void 풀을_주면_그_풀만_모르는_풀은_400() {
        service.search("MYPC0011");
        verify(mapper).selectAdminMissions("MYPC0011");

        assertThatThrownBy(() -> service.search("MYPC0099"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 보낸_칸만_바꾸고_감사_로그를_남긴다() {
        when(mapper.selectMission(3L)).thenReturn(stored(3L, 5, 1, "Y"), stored(3L, 20, 1, "N"));

        AdminCareMissionSummary updated = service.update(ADMIN, 3L, new AdminCareMissionUpdateRequest(20, null, false));

        ArgumentCaptor<CareMsn> captor = ArgumentCaptor.forClass(CareMsn.class);
        verify(mapper).updateMission(captor.capture());
        CareMsn sent = captor.getValue();
        assertThat(sent.getCareMsnSn()).isEqualTo(3L);
        assertThat(sent.getCmpnPnt()).isEqualTo(20);
        assertThat(sent.getAchvCrtrNmtm()).isNull();
        assertThat(sent.getUseYn()).isEqualTo("N");
        assertThat(sent.getMdfrId()).isEqualTo("1");
        assertThat(sent.getMsnNm()).isNull();
        assertThat(sent.getMsnPoolCd()).isNull();
        assertThat(sent.getMvmnPath()).isNull();
        assertThat(updated.cmpnPnt()).isEqualTo(20);
        assertThat(updated.useYn()).isFalse();
        verify(adminTaskLogService).record(eq(ADMIN), eq(AdminTaskType.UPDATE), eq("CARE_MSN"), eq(3L),
                anyString(), any(AdminCareMissionSummary.class), eq(updated));
    }

    @Test
    void 아무_칸도_없으면_400() {
        assertThatThrownBy(() -> service.update(ADMIN, 3L, new AdminCareMissionUpdateRequest(null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updateMission(any());
    }

    @Test
    void 없는_미션은_404() {
        assertThatThrownBy(() -> service.update(ADMIN, 99L, new AdminCareMissionUpdateRequest(10, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_MISSION_NOT_FOUND);
        verify(mapper, never()).updateMission(any());
    }
}
