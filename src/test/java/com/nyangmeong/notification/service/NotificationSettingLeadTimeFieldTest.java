package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.notification.dto.NotificationSettingUpdateRequest;
import com.nyangmeong.notification.mapper.NotificationMapper;

/**
 * [검사 - 알림 설정 저장 400 칸 이름]
 * - 케어 일정 ON인데 사전 알림 시점이 비면 data[].field = leadTimes
 */
class NotificationSettingLeadTimeFieldTest {

    private final NotificationService notificationService = new NotificationService(mock(NotificationMapper.class),
            mock(AuthService.class));

    @Test
    void 케어_일정_ON에_사전_알림_시점이_없으면_leadTimes_칸으로_알린다() {
        NotificationSettingUpdateRequest req = new NotificationSettingUpdateRequest(true, true, true, List.of(), true,
                true, true, true);

        assertThatThrownBy(() -> notificationService.updateSetting(1L, req))
                .isInstanceOfSatisfying(CustomException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    assertThat(e.getFields()).containsExactly("leadTimes");
                });
    }
}
