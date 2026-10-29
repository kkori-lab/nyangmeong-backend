package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.ai.dto.AiSettingResponse;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingMapper;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - AI 설정값 조회]
 */
class AiSettingServiceTest {

    private final SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
    private final AiSettingService service = new AiSettingService(new SystemSettingService(systemSettingMapper));

    @Test
    void 설정_행의_토큰_상한과_대화_턴_수를_준다() {
        SystemSetting setting = new SystemSetting();
        setting.setAiTknUplmtCnt(3000L);
        setting.setAiCnvrsTurnCnt(4L);
        when(systemSettingMapper.selectCurrent()).thenReturn(setting);

        AiSettingResponse result = service.getSetting();

        assertThat(result.aiTknUplmtCnt()).isEqualTo(3000);
        assertThat(result.aiCnvrsTurnCnt()).isEqualTo(4);
    }

    @Test
    void 설정_행이_없으면_기본값_4500과_6이다() {
        when(systemSettingMapper.selectCurrent()).thenReturn(null);

        AiSettingResponse result = service.getSetting();

        assertThat(result.aiTknUplmtCnt()).isEqualTo(4500);
        assertThat(result.aiCnvrsTurnCnt()).isEqualTo(6);
    }
}
