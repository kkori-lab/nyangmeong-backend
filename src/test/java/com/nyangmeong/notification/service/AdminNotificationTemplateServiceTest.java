package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.notification.domain.NotificationChannel;
import com.nyangmeong.notification.domain.NotificationTemplate;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateRequest;
import com.nyangmeong.notification.dto.AdminNotificationTemplateUpdateRequest;
import com.nyangmeong.notification.mapper.NotificationMapper;

/**
 * [검사 - 관리자 알림 템플릿 등록·수정 규칙]
 */
class AdminNotificationTemplateServiceTest {

    private static final String CARE = NotificationType.CARE_SCHEDULE.getCode();
    private static final String INAPP = NotificationChannel.INAPP.getCode();

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminNotificationTemplateService service = new AdminNotificationTemplateService(
            notificationMapper, adminTaskLogService);

    private AdminNotificationTemplateCreateRequest createRequest(String notiTypeCd, String notiChnlCd,
            String notiTtl, String notiCn) {
        return new AdminNotificationTemplateCreateRequest(notiTypeCd, notiChnlCd, notiTtl, notiCn, true);
    }

    private NotificationTemplate stored(String notiTypeCd) {
        NotificationTemplate template = new NotificationTemplate();
        template.setNotiTmpltSn(3L);
        template.setNotiTypeCd(notiTypeCd);
        template.setNotiChnlCd(INAPP);
        template.setNotiTtl("옛 제목");
        template.setNotiCn("옛 본문");
        template.setUseYn("Y");
        return template;
    }

    @Test
    void 케어_일정_템플릿을_등록하고_감사_로그를_남긴다() {
        doAnswer(invocation -> {
            invocation.<NotificationTemplate>getArgument(0).setNotiTmpltSn(7L);
            return null;
        }).when(notificationMapper).insertTemplate(any());

        Long notiTmpltSn = service.create(9L,
                createRequest(CARE, INAPP, "  {{반려동물명}} {{D-N}}  ", "{{닉네임}}님 {{일정일}} {{시간}} {{병원명}}"))
                .notiTmpltSn();

        assertThat(notiTmpltSn).isEqualTo(7L);
        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(notificationMapper).insertTemplate(captor.capture());
        NotificationTemplate saved = captor.getValue();
        assertThat(saved.getNotiTtl()).isEqualTo("{{반려동물명}} {{D-N}}");
        assertThat(saved.getUseYn()).isEqualTo("Y");
        assertThat(saved.getRgtrId()).isEqualTo("9");
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.CREATE), eq("NOTI_TMPLT"), eq(7L), any(),
                isNull(), any());
    }

    @Test
    void 빈_제목은_NULL로_저장해_기본_제목을_쓴다() {
        service.create(9L, createRequest(CARE, INAPP, "  ", "본문"));

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(notificationMapper).insertTemplate(captor.capture());
        assertThat(captor.getValue().getNotiTtl()).isNull();
    }

    @Test
    void 템플릿을_읽지_않는_유형은_400_NOT_SUPPORTED() {
        assertThatThrownBy(() -> service.create(9L,
                createRequest(NotificationType.NOTICE.getCode(), INAPP, null, "본문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_TEMPLATE_NOT_SUPPORTED);
        verify(notificationMapper, never()).insertTemplate(any());
    }

    @Test
    void 모르는_채널은_400() {
        assertThatThrownBy(() -> service.create(9L, createRequest(CARE, "AIMC9999", null, "본문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 모르는_치환_변수가_있으면_400() {
        assertThatThrownBy(() -> service.create(9L, createRequest(CARE, INAPP, "{{보호자}}님", "본문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.create(9L, createRequest(CARE, INAPP, null, "{{}} 본문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(notificationMapper, never()).insertTemplate(any());
    }

    @Test
    void 같은_유형_채널이_있으면_409() {
        doThrow(new DuplicateKeyException("UK_NOTI_TMPLT_1")).when(notificationMapper).insertTemplate(any());

        assertThatThrownBy(() -> service.create(9L, createRequest(CARE, INAPP, null, "본문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_TEMPLATE_DUPLICATED);
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 수정은_세_칸을_덮어쓰고_전후를_감사_로그에_남긴다() {
        when(notificationMapper.selectTemplateBySn(3L)).thenReturn(stored(CARE));

        service.update(9L, 3L, new AdminNotificationTemplateUpdateRequest(null, "{{반려동물명}} 새 본문", false));

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(notificationMapper).updateTemplate(captor.capture());
        NotificationTemplate saved = captor.getValue();
        assertThat(saved.getNotiTtl()).isNull();
        assertThat(saved.getNotiCn()).isEqualTo("{{반려동물명}} 새 본문");
        assertThat(saved.getUseYn()).isEqualTo("N");
        assertThat(saved.getNotiTypeCd()).isEqualTo(CARE);
        assertThat(saved.getMdfrId()).isEqualTo("9");
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.UPDATE), eq("NOTI_TMPLT"), eq(3L), any(),
                any(), any());
    }

    @Test
    void 없는_템플릿_수정은_404() {
        assertThatThrownBy(() -> service.update(9L, 99L,
                new AdminNotificationTemplateUpdateRequest(null, "본문", true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
    }

    @Test
    void 수정도_모르는_치환_변수면_400() {
        when(notificationMapper.selectTemplateBySn(3L)).thenReturn(stored(CARE));

        assertThatThrownBy(() -> service.update(9L, 3L,
                new AdminNotificationTemplateUpdateRequest(null, "{{주소}} 본문", true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(notificationMapper, never()).updateTemplate(any());
    }

    @Test
    void 목록_채널이_모르는_코드면_400이고_비면_전체() {
        assertThatThrownBy(() -> service.search("AIMC9999"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        service.search(" ");
        verify(notificationMapper).selectTemplates(null);
    }
}
