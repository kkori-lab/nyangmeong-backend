package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSuspensionService;

/**
 * [검사 - 채팅 상대 신고 접수와 누적 자동 정지]
 */
class ChatReportServiceTest {

    private static final Long OWNER = 1L;
    private static final Long APPLICANT = 2L;
    private static final Long STRANGER = 3L;
    private static final Long ROOM = 40L;
    private static final Long DCLR = 55L;
    private static final String RECEIVED = ReportStatus.RECEIVED.getCode();

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final ChatRoomGuard chatRoomGuard = mock(ChatRoomGuard.class);
    private final MemberService memberService = mock(MemberService.class);
    private final MemberSuspensionService memberSuspensionService = mock(MemberSuspensionService.class);
    private final ReportService service = new ReportService(reportMapper, mock(PostMapper.class),
            mock(CommentMapper.class), systemSettingService, chatRoomGuard, memberService, memberSuspensionService);

    private void stubRoom() {
        ChatRoomDetail room = new ChatRoomDetail();
        room.setOwnerMbrSn(OWNER);
        room.setCtptMbrSn(APPLICANT);
        when(chatRoomGuard.getRoom(OWNER, ROOM)).thenReturn(room);
        when(chatRoomGuard.getRoom(APPLICANT, ROOM)).thenReturn(room);
        when(chatRoomGuard.getRoom(STRANGER, ROOM)).thenThrow(new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        doAnswer(invocation -> {
            Report arg = invocation.getArgument(0);
            arg.setDclrSn(DCLR);
            return 1;
        }).when(reportMapper).insertReport(any(Report.class));
    }

    private ReportCreateRequest chat(String dclrRsnCd, String dclrDtlCn) {
        return new ReportCreateRequest(null, null, ROOM, dclrRsnCd, dclrDtlCn);
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private Report insertedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insertReport(captor.capture());
        return captor.getValue();
    }

    @Test
    void 보호자가_신고하면_지원자가_대상이고_접수_상태로_저장한다() {
        stubRoom();

        ReportResponse res = service.create(OWNER, chat("CMUC0014", "욕설을 했어요"));

        Report saved = insertedReport();
        assertThat(saved.getMbrSn()).isEqualTo(OWNER);
        assertThat(saved.getOtoChatRoomSn()).isEqualTo(ROOM);
        assertThat(saved.getTrgtMbrSn()).isEqualTo(APPLICANT);
        assertThat(saved.getPstSn()).isNull();
        assertThat(saved.getCmntSn()).isNull();
        assertThat(saved.getDclrDtlCn()).isEqualTo("욕설을 했어요");
        assertThat(res.dclrPrcsSttsCd()).isEqualTo(RECEIVED);
        ArgumentCaptor<ReportStatusHistory> history = ArgumentCaptor.forClass(ReportStatusHistory.class);
        verify(reportMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(RECEIVED);
        verify(memberService).checkNotWithdrawn(APPLICANT);
    }

    @Test
    void 지원자가_신고하면_보호자가_대상이다() {
        stubRoom();

        service.create(APPLICANT, chat("CMUC0010", null));

        assertThat(insertedReport().getTrgtMbrSn()).isEqualTo(OWNER);
    }

    @Test
    void 참여자가_아니면_403이고_저장하지_않는다() {
        stubRoom();

        assertError(() -> service.create(STRANGER, chat("CMUC0010", null)), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        verify(reportMapper, never()).insertReport(any());
    }

    @Test
    void 탈퇴한_상대는_404다() {
        stubRoom();
        doThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND)).when(memberService).checkNotWithdrawn(APPLICANT);

        assertError(() -> service.create(OWNER, chat("CMUC0010", null)), ErrorCode.MEMBER_NOT_FOUND);
        verify(reportMapper, never()).insertReport(any());
    }

    @Test
    void 같은_방을_다시_신고하면_409다() {
        stubRoom();
        when(reportMapper.existsChatReport(OWNER, ROOM)).thenReturn(true);

        assertError(() -> service.create(OWNER, chat("CMUC0010", null)), ErrorCode.REPORT_CHAT_ALREADY_EXISTS);
        verify(reportMapper, never()).insertReport(any());
    }

    @Test
    void 동시에_같은_방을_신고해_유일_키에_걸려도_409다() {
        stubRoom();
        doThrow(new DuplicateKeyException("UK_DCLR_3")).when(reportMapper).insertReport(any(Report.class));

        assertError(() -> service.create(OWNER, chat("CMUC0010", null)), ErrorCode.REPORT_CHAT_ALREADY_EXISTS);
    }

    @Test
    void 대상은_글_댓글_채팅방_중_하나만이고_기타는_상세가_필수다() {
        assertError(() -> service.create(OWNER, new ReportCreateRequest(7L, null, ROOM, "CMUC0010", null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(OWNER, new ReportCreateRequest(null, 9L, ROOM, "CMUC0010", null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(OWNER, chat("CMUC0014", " ")), ErrorCode.INVALID_INPUT_VALUE);
        verify(reportMapper, never()).insertReport(any());
    }

    @Test
    void 접수_신고자_수가_기준_직전이면_정지하지_않는다() {
        stubRoom();
        when(reportMapper.countReportersOfMember(APPLICANT, RECEIVED)).thenReturn(4);

        service.create(OWNER, chat("CMUC0010", null));

        verify(memberSuspensionService, never()).suspendBySystem(anyLong(), anyInt(), anyString());
    }

    @Test
    void 접수_신고자_수가_기준에_닿으면_7일_자동_정지한다() {
        stubRoom();
        when(reportMapper.countReportersOfMember(APPLICANT, RECEIVED)).thenReturn(5);

        service.create(OWNER, chat("CMUC0010", null));

        verify(memberSuspensionService).suspendBySystem(APPLICANT, 7, ReportService.AUTO_SUSPEND_RSN);
    }

    @Test
    void 정지할_수_없는_회원이어도_신고는_저장된다() {
        stubRoom();
        when(reportMapper.countReportersOfMember(APPLICANT, RECEIVED)).thenReturn(6);
        when(memberSuspensionService.suspendBySystem(APPLICANT, 7, ReportService.AUTO_SUSPEND_RSN))
                .thenReturn(false);

        ReportResponse res = service.create(OWNER, chat("CMUC0010", null));

        assertThat(res.dclrSn()).isEqualTo(DCLR);
    }

    @Test
    void 채팅_신고는_글_댓글_누적_처리를_하지_않는다() {
        stubRoom();

        service.create(OWNER, chat("CMUC0010", null));

        verify(reportMapper, never()).countAccumulatedReports(any(), any(), any());
    }
}
