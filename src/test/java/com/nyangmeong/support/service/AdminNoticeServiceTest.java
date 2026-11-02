package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NoticeNotificationEvent;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.support.domain.Notice;
import com.nyangmeong.support.dto.AdminNoticeCreateRequest;
import com.nyangmeong.support.dto.AdminNoticeResponse;
import com.nyangmeong.support.dto.AdminNoticeSearchCond;
import com.nyangmeong.support.dto.AdminNoticeUpdateRequest;
import com.nyangmeong.support.mapper.NoticeMapper;

/**
 * [검사 - 관리자 공지 목록·등록·알림 발송·수정]
 */
class AdminNoticeServiceTest {

    private final NoticeMapper noticeMapper = mock(NoticeMapper.class);
    private final MemberService memberService = mock(MemberService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminNoticeService service = new AdminNoticeService(noticeMapper, memberService, eventPublisher,
            fileStore, adminTaskLogService);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Notice stored(Long sn, String useYn) {
        Notice notice = new Notice();
        notice.setNtcMttrSn(sn);
        notice.setNtcMttrTtl("점검 안내");
        notice.setNtcMttrCn("본문");
        notice.setUpendFixYn("Y");
        notice.setUseYn(useYn);
        notice.setInqCnt(0);
        return notice;
    }

    /** insert 가 번호를 채우고 단건 조회가 그 공지를 돌려주게 한다 */
    private void givenInsert(Long sn) {
        doAnswer(invocation -> {
            ((Notice) invocation.getArgument(0)).setNtcMttrSn(sn);
            return 1;
        }).when(noticeMapper).insert(any());
        when(noticeMapper.selectAdminNotice(sn)).thenReturn(stored(sn, "Y"));
    }

    @Test
    void 목록은_검색어를_다듬고_LIKE_문자를_이스케이프한다() {
        AdminNoticeSearchCond cond = new AdminNoticeSearchCond();
        cond.setKeyword(" 50%_할인 ");
        service.search(cond, new PageQuery(null, null, null));
        verify(noticeMapper).selectAdminList("50\\%\\_할인");

        cond.setKeyword("  ");
        service.search(cond, new PageQuery(null, null, null));
        verify(noticeMapper).selectAdminList(null);
    }

    @Test
    void 발송을_체크하면_전체_알림_이벤트를_한_번만_발행하고_대상_수를_남긴다() {
        givenInsert(7L);
        when(memberService.countNormalMembers()).thenReturn(191);

        AdminNoticeResponse res = service.create(9L,
                new AdminNoticeCreateRequest(" 점검 안내 ", "본문", true, true, true));

        ArgumentCaptor<NoticeNotificationEvent> event = ArgumentCaptor.forClass(NoticeNotificationEvent.class);
        verify(eventPublisher, times(1)).publish(event.capture());
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
        assertThat(event.getValue().notiCn()).isEqualTo("점검 안내");
        assertThat(event.getValue().mvmnPath()).isEqualTo("/notices/7");
        verify(noticeMapper).updateNotificationSent(7L, 191);
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.CREATE), eq("NTC_MTTR"), eq(7L), any(),
                any(), any());
        assertThat(res.ntcMttrSn()).isEqualTo(7L);
    }

    @Test
    void 등록은_제목을_다듬고_등록자를_관리자_번호로_남긴다() {
        givenInsert(7L);

        service.create(9L, new AdminNoticeCreateRequest(" 점검 안내 ", "본문", false, true, false));

        ArgumentCaptor<Notice> inserted = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getNtcMttrTtl()).isEqualTo("점검 안내");
        assertThat(inserted.getValue().getUpendFixYn()).isEqualTo("N");
        assertThat(inserted.getValue().getUseYn()).isEqualTo("Y");
        assertThat(inserted.getValue().getRgtrId()).isEqualTo("9");
    }

    @Test
    void 발송을_체크하지_않으면_알림을_보내지_않는다() {
        givenInsert(7L);

        service.create(9L, new AdminNoticeCreateRequest("제목", "본문", false, true, false));

        verify(eventPublisher, never()).publish(any(NoticeNotificationEvent.class));
        verify(memberService, never()).countNormalMembers();
        verify(noticeMapper, never()).updateNotificationSent(anyLong(), anyInt());
    }

    @Test
    void 비노출_공지에_알림_발송을_함께_보내면_400() {
        assertThatThrownBy(() -> service.create(9L, new AdminNoticeCreateRequest("제목", "본문", false, false, true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(noticeMapper, never()).insert(any());
        verify(eventPublisher, never()).publish(any(NoticeNotificationEvent.class));
    }

    @Test
    void 상단_고정은_개수를_검사하지_않는다() {
        givenInsert(7L);

        for (int i = 0; i < 3; i++) {
            service.create(9L, new AdminNoticeCreateRequest("고정 " + i, "본문", true, true, false));
        }

        verify(noticeMapper, times(3)).insert(any());
    }

    @Test
    void 수정은_알림을_다시_보내지_않고_보낸_값만_바꾼다() {
        when(noticeMapper.selectAdminNotice(5L)).thenReturn(stored(5L, "Y"), stored(5L, "N"));

        AdminNoticeResponse res = service.update(9L, 5L, new AdminNoticeUpdateRequest(null, null, null, false));

        ArgumentCaptor<Notice> updated = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).update(updated.capture());
        assertThat(updated.getValue().getUseYn()).isEqualTo("N");
        assertThat(updated.getValue().getNtcMttrTtl()).isNull();
        assertThat(updated.getValue().getUpendFixYn()).isNull();
        assertThat(updated.getValue().getMdfrId()).isEqualTo("9");
        verify(eventPublisher, never()).publish(any(NoticeNotificationEvent.class));
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.UPDATE), eq("NTC_MTTR"), eq(5L), any(),
                any(), any());
        assertThat(res.useYn()).isFalse();
    }

    @Test
    void 없는_공지는_404_NOTICE_NOT_FOUND() {
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        assertThatThrownBy(() -> service.update(9L, 99L, new AdminNoticeUpdateRequest("제목", null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
        verify(noticeMapper, never()).update(any());
    }

    @Test
    void 비노출_공지도_관리자_상세로_조회된다() {
        when(noticeMapper.selectAdminNotice(5L)).thenReturn(stored(5L, "N"));

        assertThat(service.get(5L).useYn()).isFalse();
    }

    @Test
    void 알림_대상_수는_정상_회원_수다() {
        when(memberService.countNormalMembers()).thenReturn(1270);

        assertThat(service.getNotificationTargetCount().trgtCnt()).isEqualTo(1270);
    }

    @Test
    void 이미지가_없으면_400() {
        assertThatThrownBy(() -> service.uploadImage(9L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(fileStore, never()).storePublic(any(org.springframework.web.multipart.MultipartFile.class), any());
    }
}
