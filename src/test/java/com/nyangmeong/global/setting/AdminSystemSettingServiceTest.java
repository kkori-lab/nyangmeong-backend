package com.nyangmeong.global.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.security.domain.LoginMember;

/**
 * [검사 - 운영 설정 수정]
 */
class AdminSystemSettingServiceTest {

    private final SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminSystemSettingService service = new AdminSystemSettingService(
            new SystemSettingService(systemSettingMapper), systemSettingMapper, adminTaskLogService);
    private final LoginMember admin = new LoginMember(9L, "USRC0005");

    private AdminSettingUpdateRequest request(Long petMaxCnt, Long fileMaxSz, String aiCmtryBscCn) {
        return new AdminSettingUpdateRequest(petMaxCnt, null, null, null, null, null, null, null, null, null, null,
                null, null, aiCmtryBscCn, null, null, null, fileMaxSz);
    }

    @SuppressWarnings("unchecked")
    @Test
    void 바뀐_항목만_저장하고_전후_값을_로그로_남긴다() {
        SystemSetting current = new SystemSetting();
        when(systemSettingMapper.selectCurrent()).thenReturn(current);

        service.update(admin, request(20L, current.getFileMaxSz(), null));

        ArgumentCaptor<SystemSetting> saved = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingMapper).upsert(saved.capture(), eq("9"));
        assertThat(saved.getValue().getPetMaxCnt()).isEqualTo(20L);

        ArgumentCaptor<Object> before = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> after = ArgumentCaptor.forClass(Object.class);
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.UPDATE), eq("SYS_STNG"), eq(1L),
                eq("운영 설정 수정: petMaxCnt"), before.capture(), after.capture());
        assertThat((Map<String, Object>) before.getValue()).containsOnlyKeys("petMaxCnt")
                .containsEntry("petMaxCnt", 10L);
        assertThat((Map<String, Object>) after.getValue()).containsOnlyKeys("petMaxCnt")
                .containsEntry("petMaxCnt", 20L);
    }

    @Test
    void 설정_행이_없으면_기본값_위에_덮어써_저장한다() {
        when(systemSettingMapper.selectCurrent()).thenReturn(null);

        service.update(admin, request(null, 5_242_880L, null));

        ArgumentCaptor<SystemSetting> saved = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingMapper).upsert(saved.capture(), eq("9"));
        assertThat(saved.getValue().getFileMaxSz()).isEqualTo(5_242_880L);
        assertThat(saved.getValue().getPetMaxCnt()).isEqualTo(10L);
    }

    @Test
    void 바뀐_게_없으면_저장도_로그도_없다() {
        SystemSetting current = new SystemSetting();
        when(systemSettingMapper.selectCurrent()).thenReturn(current);

        SystemSetting result = service.update(admin, request(10L, null, current.getAiCmtryBscCn()));

        assertThat(result.getPetMaxCnt()).isEqualTo(10L);
        verify(systemSettingMapper, never()).upsert(any(), any());
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 아무것도_보내지_않아도_바뀐_게_없는_것() {
        when(systemSettingMapper.selectCurrent()).thenReturn(new SystemSetting());

        service.update(admin, request(null, null, null));

        verify(systemSettingMapper, never()).upsert(any(), any());
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }
}
