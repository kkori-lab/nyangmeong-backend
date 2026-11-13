package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.CareCompletionRequest;
import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.CareMatchCondition;
import com.nyangmeong.community.domain.CareMatchStatus;
import com.nyangmeong.community.domain.CareMatchStatusHistory;
import com.nyangmeong.community.domain.CareRecruitment;
import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentStatusHistory;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.dto.CareCompletionRequestCreateRequest;
import com.nyangmeong.community.dto.CareMatchCancelRequest;
import com.nyangmeong.community.dto.CareMatchCreateRequest;
import com.nyangmeong.community.dto.CareMatchReceiptConfirmRequest;
import com.nyangmeong.community.dto.CareMatchResponse;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;

/**
 * [검사 - 펫돌봄 매칭·거래 상태 전이]
 * - 허용(역할·단계가 맞을 때)과 거부(역할이 틀림 403, 순서를 건너뜀 409)를 단계마다 확인
 */
class CareMatchServiceTest {

    private static final Long GUARDIAN = 1L;
    private static final Long SITTER = 2L;
    private static final Long PST = 7L;
    private static final Long RCRT = 5L;
    private static final Long ROOM = 11L;
    private static final Long MATCH = 21L;
    private static final String NORMAL = "CMUC0003";

    private static final String REQUESTED = "PCAC0012";
    private static final String MATCHED = "PCAC0013";
    private static final String AWAITING_CONFIRM = "PCAC0014";
    private static final String AWAITING_RECEIPT = "PCAC0015";
    private static final String COMPLETED = "PCAC0016";
    private static final String CANCELED = "PCAC0017";
    private static final String RECRUITING_CD = "PCAC0001";
    private static final String MATCHED_CD = "PCAC0002";
    private static final String COMPLETED_CD = "PCAC0003";

    private final CareMatchMapper careMatchMapper = mock(CareMatchMapper.class);
    private final CareRecruitmentMapper careRecruitmentMapper = mock(CareRecruitmentMapper.class);
    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
    private final ChatViewRegistry chatViewRegistry = mock(ChatViewRegistry.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final CareMatchService service = new CareMatchService(careMatchMapper, careRecruitmentMapper,
            chatRoomMapper, new ChatRoomGuard(chatRoomMapper), chatMessageService, chatViewRegistry,
            eventPublisher);

    @BeforeEach
    void stubCommon() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null, null));
        when(chatRoomMapper.selectParticipant(eq(ROOM), anyLong())).thenAnswer(inv -> {
            ChatParticipant participant = new ChatParticipant();
            participant.setPtcpSttsCd("CHTC0003");
            return participant;
        });
        when(careMatchMapper.updateStatus(anyLong(), anyString(), anyString())).thenReturn(1);
        when(careMatchMapper.updateRequestAgain(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);
        when(careRecruitmentMapper.updateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(1);
        when(careMatchMapper.selectNextCndVerNo(MATCH)).thenReturn(1);
        when(careMatchMapper.selectNextDmndCycl(MATCH)).thenReturn(1);
        when(careMatchMapper.selectLatestCondition(MATCH)).thenReturn(condition());
        when(careMatchMapper.selectCompletionRequests(MATCH)).thenReturn(List.of());
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment(RECRUITING_CD));
    }

    private ChatRoomDetail room(Long petPrtcMtchSn, String mtchDlngSttsCd) {
        ChatRoomDetail detail = new ChatRoomDetail();
        detail.setOtoChatRoomSn(ROOM);
        detail.setPstSn(PST);
        detail.setCtptMbrSn(SITTER);
        detail.setOwnerMbrSn(GUARDIAN);
        detail.setPetPrtcRcrtSn(RCRT);
        detail.setPetPrtcMtchSn(petPrtcMtchSn);
        detail.setMtchDlngSttsCd(mtchDlngSttsCd);
        return detail;
    }

    private CareRecruitment recruitment(String sttsCd) {
        CareRecruitment recruitment = new CareRecruitment();
        recruitment.setPetPrtcRcrtSn(RCRT);
        recruitment.setPstSn(PST);
        recruitment.setRcrtSttsCd(sttsCd);
        recruitment.setCstUnitCd("PCAC0005");
        recruitment.setCstAmt(8000);
        recruitment.setSchdlCnsltnPsbltyYn("N");
        recruitment.setPrtcBgngDt(LocalDateTime.of(2026, 11, 20, 19, 0));
        recruitment.setPrtcEndDt(LocalDateTime.of(2026, 11, 20, 19, 30));
        return recruitment;
    }

    private CareMatchCondition condition() {
        CareMatchCondition condition = new CareMatchCondition();
        condition.setMtchCndVerSn(90L);
        condition.setPetPrtcMtchSn(MATCH);
        condition.setCndVerNo(1);
        condition.setCstUnitCd("PCAC0005");
        condition.setAcrdCstAmt(8000);
        condition.setAcrdPrtcCn("저녁 산책");
        condition.setSchdlCnsltnPsbltyYn("N");
        return condition;
    }

    /** 매칭을 지정한 상태로 둔다 */
    private CareMatch stubMatch(String sttsCd) {
        CareMatch match = new CareMatch();
        match.setPetPrtcMtchSn(MATCH);
        match.setPetPrtcRcrtSn(RCRT);
        match.setOtoChatRoomSn(ROOM);
        match.setMtchDlngSttsCd(sttsCd);
        when(careMatchMapper.selectById(MATCH)).thenReturn(match);
        when(careMatchMapper.selectByIdForUpdate(MATCH)).thenReturn(match);
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(MATCH, sttsCd));
        return match;
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private CareMatchCreateRequest createReq() {
        return new CareMatchCreateRequest(ROOM, "req-1");
    }

    private void stubPost() {
        CareRecruitmentDetail post = new CareRecruitmentDetail();
        post.setPstCn("저녁 산책");
        when(careRecruitmentMapper.selectDetailByPstSn(PST, NORMAL)).thenReturn(post);
    }

    // 매칭 요청

    @Test
    void 보호자의_매칭_요청은_모집글_조건을_복사해_수락_대기로_만든다() {
        stubPost();
        doAnswer(inv -> {
            inv.<CareMatch>getArgument(0).setPetPrtcMtchSn(MATCH);
            return 1;
        }).when(careMatchMapper).insertMatch(any(CareMatch.class));
        doAnswer(inv -> {
            inv.<CareMatchCondition>getArgument(0).setMtchCndVerSn(90L);
            return 1;
        }).when(careMatchMapper).insertCondition(any(CareMatchCondition.class));

        CareMatchResponse res = service.request(GUARDIAN, createReq());

        assertThat(res.mtchDlngSttsCd()).isEqualTo(REQUESTED);
        ArgumentCaptor<CareMatchCondition> cnd = ArgumentCaptor.forClass(CareMatchCondition.class);
        verify(careMatchMapper).insertCondition(cnd.capture());
        assertThat(cnd.getValue().getCndVerNo()).isEqualTo(1);
        assertThat(cnd.getValue().getAcrdCstAmt()).isEqualTo(8000);
        assertThat(cnd.getValue().getAcrdPrtcCn()).isEqualTo("저녁 산책");
        ArgumentCaptor<CareMatchStatusHistory> history = ArgumentCaptor.forClass(CareMatchStatusHistory.class);
        verify(careMatchMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(REQUESTED);
        verify(chatMessageService).createSystemMessage(eq(ROOM), anyString());
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(SITTER);
        assertThat(event.getValue().type()).isEqualTo(NotificationType.CARE_MATCH);
        assertThat(event.getValue().mvmnPath()).isEqualTo("/chat?otoChatRoomSn=" + ROOM);
    }

    @Test
    void 지원자는_매칭을_요청할_수_없다() {
        assertError(() -> service.request(SITTER, createReq()), ErrorCode.CARE_MATCH_ROLE_DENIED);
        verify(careMatchMapper, never()).insertMatch(any());
    }

    @Test
    void 참여자가_아니면_매칭을_요청할_수_없다() {
        assertError(() -> service.request(99L, createReq()), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @Test
    void 모집중이_아니면_매칭을_요청할_수_없다() {
        stubPost();
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment(MATCHED_CD));

        assertError(() -> service.request(GUARDIAN, createReq()), ErrorCode.CARE_RECRUITMENT_CLOSED);
    }

    @Test
    void 요청_취소된_매칭은_같은_행으로_다시_요청하고_조건_버전을_새로_만든다() {
        stubPost();
        CareMatch canceled = stubMatch(CANCELED);
        when(careMatchMapper.selectByRoomForUpdate(ROOM)).thenReturn(canceled);
        when(careMatchMapper.selectNextCndVerNo(MATCH)).thenReturn(2);

        service.request(GUARDIAN, createReq());

        verify(careMatchMapper, never()).insertMatch(any());
        verify(careMatchMapper).updateRequestAgain(MATCH, "req-1", CANCELED, REQUESTED);
        ArgumentCaptor<CareMatchCondition> cnd = ArgumentCaptor.forClass(CareMatchCondition.class);
        verify(careMatchMapper).insertCondition(cnd.capture());
        assertThat(cnd.getValue().getCndVerNo()).isEqualTo(2);
    }

    @Test
    void 이미_진행_중인_매칭은_다시_요청할_수_없다() {
        stubPost();
        CareMatch matched = stubMatch(MATCHED);
        when(careMatchMapper.selectByRoomForUpdate(ROOM)).thenReturn(matched);

        assertError(() -> service.request(GUARDIAN, createReq()), ErrorCode.CARE_MATCH_INVALID_STATE);
    }

    @Test
    void 같은_멱등_요청_ID는_기존_매칭을_돌려주고_새로_만들지_않는다() {
        CareMatch existing = stubMatch(REQUESTED);
        when(careMatchMapper.selectByIdmptDmndIdForShare("req-1")).thenReturn(existing);

        CareMatchResponse res = service.request(GUARDIAN, createReq());

        assertThat(res.petPrtcMtchSn()).isEqualTo(MATCH);
        verify(careMatchMapper, never()).insertMatch(any());
        verify(chatMessageService, never()).createSystemMessage(anyLong(), anyString());
    }

    // 요청 취소

    @Test
    void 보호자는_수락_대기에서_사유와_함께_취소한다() {
        stubMatch(REQUESTED);

        service.cancel(GUARDIAN, MATCH, new CareMatchCancelRequest("일정이 맞지 않아요"));

        verify(careMatchMapper).updateStatus(MATCH, REQUESTED, CANCELED);
        ArgumentCaptor<CareMatchStatusHistory> history = ArgumentCaptor.forClass(CareMatchStatusHistory.class);
        verify(careMatchMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getChgRsn()).isEqualTo("일정이 맞지 않아요");
        assertThat(history.getValue().getMbrSn()).isEqualTo(GUARDIAN);
    }

    @Test
    void 취소는_수락_대기가_아니면_거부한다() {
        stubMatch(MATCHED);

        assertError(() -> service.cancel(GUARDIAN, MATCH, new CareMatchCancelRequest("사유")),
                ErrorCode.CARE_MATCH_INVALID_STATE);
    }

    @Test
    void 제공자는_요청을_취소할_수_없다() {
        stubMatch(REQUESTED);

        assertError(() -> service.cancel(SITTER, MATCH, new CareMatchCancelRequest("사유")),
                ErrorCode.CARE_MATCH_ROLE_DENIED);
    }

    // 매칭 수락

    @Test
    void 제공자가_수락하면_매칭_완료이고_모집글이_매칭완료로_바뀐다() {
        stubMatch(REQUESTED);
        when(chatRoomMapper.selectOtherRoomSns(PST, ROOM)).thenReturn(List.of(12L));
        CareMatch other = new CareMatch();
        other.setPetPrtcMtchSn(22L);
        other.setMtchDlngSttsCd(REQUESTED);
        when(careMatchMapper.selectByRoomForUpdate(12L)).thenReturn(other);

        service.accept(SITTER, MATCH);

        verify(careMatchMapper).updateStatus(MATCH, REQUESTED, MATCHED);
        verify(careRecruitmentMapper).updateStatus(RCRT, RECRUITING_CD, MATCHED_CD, MATCH);
        ArgumentCaptor<CareRecruitmentStatusHistory> history = ArgumentCaptor
                .forClass(CareRecruitmentStatusHistory.class);
        verify(careRecruitmentMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(MATCHED_CD);
        /** 같은 글의 다른 방은 요청이 취소되고 모집 종료 안내를 받는다 */
        verify(careMatchMapper).updateStatus(22L, REQUESTED, CANCELED);
        verify(chatMessageService).createSystemMessage(eq(12L), anyString());
    }

    @Test
    void 이미_다른_제공자가_확정된_글은_수락할_수_없다() {
        stubMatch(REQUESTED);
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment(MATCHED_CD));

        assertError(() -> service.accept(SITTER, MATCH), ErrorCode.CARE_MATCH_ALREADY_MATCHED);
        verify(careMatchMapper, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void 동시_수락으로_모집_상태_갱신이_0건이면_거부한다() {
        stubMatch(REQUESTED);
        when(careRecruitmentMapper.updateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(0);

        assertError(() -> service.accept(SITTER, MATCH), ErrorCode.CARE_MATCH_ALREADY_MATCHED);
    }

    @Test
    void 보호자는_수락할_수_없다() {
        stubMatch(REQUESTED);

        assertError(() -> service.accept(GUARDIAN, MATCH), ErrorCode.CARE_MATCH_ROLE_DENIED);
    }

    @Test
    void 수락은_수락_대기에서만_된다() {
        stubMatch(MATCHED);

        assertError(() -> service.accept(SITTER, MATCH), ErrorCode.CARE_MATCH_INVALID_STATE);
    }

    // 돌봄 완료 요청

    @Test
    void 제공자는_매칭_완료에서_완료를_요청해_확인_대기로_바꾼다() {
        stubMatch(MATCHED);

        service.requestCompletion(SITTER, MATCH, new CareCompletionRequestCreateRequest("cmp-1"));

        verify(careMatchMapper).updateStatus(MATCH, MATCHED, AWAITING_CONFIRM);
        ArgumentCaptor<CareCompletionRequest> cycle = ArgumentCaptor.forClass(CareCompletionRequest.class);
        verify(careMatchMapper).insertCompletionRequest(cycle.capture());
        assertThat(cycle.getValue().getDmndCycl()).isEqualTo(1);
        assertThat(cycle.getValue().getHnovCmptnIdntyYn()).isEqualTo("Y");
        assertThat(cycle.getValue().getCmptnCn()).isEmpty();
        assertThat(cycle.getValue().getMtchCndVerSn()).isEqualTo(90L);
    }

    @Test
    void 완료_요청은_매칭_완료가_아니면_거부한다() {
        stubMatch(REQUESTED);

        assertError(() -> service.requestCompletion(SITTER, MATCH, new CareCompletionRequestCreateRequest("c")),
                ErrorCode.CARE_MATCH_INVALID_STATE);
        verify(careMatchMapper, never()).insertCompletionRequest(any());
    }

    @Test
    void 보호자는_완료를_요청할_수_없다() {
        stubMatch(MATCHED);

        assertError(() -> service.requestCompletion(GUARDIAN, MATCH, new CareCompletionRequestCreateRequest("c")),
                ErrorCode.CARE_MATCH_ROLE_DENIED);
    }

    // 돌봄 완료 확인

    @Test
    void 보호자가_완료를_확인하면_수령_대기로_바뀌고_최근_회차에_확인이_기록된다() {
        stubMatch(AWAITING_CONFIRM);
        CareCompletionRequest cycle = new CareCompletionRequest();
        cycle.setPrtcCmptnDmndCyclSn(31L);
        when(careMatchMapper.selectLatestCompletionRequestForUpdate(MATCH)).thenReturn(cycle);

        service.confirmCompletion(GUARDIAN, MATCH);

        verify(careMatchMapper).updateCompletionConfirm(31L, "PCAC0019");
        verify(careMatchMapper).updateStatus(MATCH, AWAITING_CONFIRM, AWAITING_RECEIPT);
    }

    @Test
    void 완료_확인은_확인_대기가_아니면_거부한다() {
        stubMatch(MATCHED);

        assertError(() -> service.confirmCompletion(GUARDIAN, MATCH), ErrorCode.CARE_MATCH_INVALID_STATE);
    }

    @Test
    void 제공자는_완료를_확인할_수_없다() {
        stubMatch(AWAITING_CONFIRM);

        assertError(() -> service.confirmCompletion(SITTER, MATCH), ErrorCode.CARE_MATCH_ROLE_DENIED);
    }

    // 비용 수령 확인

    @Test
    void 수령을_확인하면_거래_완료이고_모집글도_거래완료로_바뀐다() {
        stubMatch(AWAITING_RECEIPT);
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment(MATCHED_CD));

        service.confirmReceipt(SITTER, MATCH, new CareMatchReceiptConfirmRequest(true));

        verify(careMatchMapper).updateReceipt(MATCH, "Y");
        verify(careMatchMapper).updateStatus(MATCH, AWAITING_RECEIPT, COMPLETED);
        verify(careRecruitmentMapper).updateStatus(RCRT, MATCHED_CD, COMPLETED_CD, MATCH);
    }

    @Test
    void 수령_전이라고_답하면_상태를_유지한다() {
        stubMatch(AWAITING_RECEIPT);

        service.confirmReceipt(SITTER, MATCH, new CareMatchReceiptConfirmRequest(false));

        verify(careMatchMapper).updateReceipt(MATCH, "N");
        verify(careMatchMapper, never()).updateStatus(anyLong(), anyString(), anyString());
        verify(careRecruitmentMapper, never()).updateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 수령_확인은_수령_대기가_아니면_거부한다() {
        stubMatch(AWAITING_CONFIRM);

        assertError(() -> service.confirmReceipt(SITTER, MATCH, new CareMatchReceiptConfirmRequest(true)),
                ErrorCode.CARE_MATCH_INVALID_STATE);
    }

    @Test
    void 보호자는_수령을_확인할_수_없다() {
        stubMatch(AWAITING_RECEIPT);

        assertError(() -> service.confirmReceipt(GUARDIAN, MATCH, new CareMatchReceiptConfirmRequest(true)),
                ErrorCode.CARE_MATCH_ROLE_DENIED);
    }

    // 알림 억제

    @Test
    void 상대가_그_방을_보는_중이면_알림을_만들지_않는다() {
        stubMatch(REQUESTED);
        when(chatViewRegistry.isViewing(GUARDIAN, ROOM)).thenReturn(true);
        when(chatRoomMapper.selectOtherRoomSns(PST, ROOM)).thenReturn(List.of());

        service.accept(SITTER, MATCH);

        verify(chatMessageService).createSystemMessage(eq(ROOM), anyString());
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 상대가_보는_중이_아니면_알림을_발행한다() {
        stubMatch(REQUESTED);
        when(chatRoomMapper.selectOtherRoomSns(PST, ROOM)).thenReturn(List.of());

        service.accept(SITTER, MATCH);

        verify(eventPublisher).publish(any(NotificationEvent.class));
    }

    // 나가기

    @Test
    void 매칭_완료_이후에_나가면_매칭이_취소되고_모집글이_모집중으로_돌아간다() {
        CareMatch match = stubMatch(AWAITING_CONFIRM);
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment(MATCHED_CD));

        service.cancelOnLeave(SITTER, room(MATCH, AWAITING_CONFIRM));

        assertThat(match.getMtchDlngSttsCd()).isEqualTo(CANCELED);
        verify(careMatchMapper).updateStatus(MATCH, AWAITING_CONFIRM, CANCELED);
        verify(careRecruitmentMapper).updateStatus(RCRT, MATCHED_CD, RECRUITING_CD, null);
    }

    @Test
    void 수락_대기에서_나가면_모집_상태는_건드리지_않는다() {
        stubMatch(REQUESTED);

        service.cancelOnLeave(GUARDIAN, room(MATCH, REQUESTED));

        verify(careMatchMapper).updateStatus(MATCH, REQUESTED, CANCELED);
        verify(careRecruitmentMapper, never()).updateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 매칭이_없거나_끝난_방에서_나가면_아무것도_바꾸지_않는다() {
        service.cancelOnLeave(GUARDIAN, room(null, null));
        stubMatch(COMPLETED);
        service.cancelOnLeave(GUARDIAN, room(MATCH, COMPLETED));

        verify(careMatchMapper, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void 참여자만_매칭을_조회한다() {
        stubMatch(REQUESTED);

        assertThat(service.getMatch(GUARDIAN, MATCH).cnd().acrdCstAmt()).isEqualTo(8000);
        assertError(() -> service.getMatch(99L, MATCH), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @Test
    void 없는_매칭은_404다() {
        assertError(() -> service.getMatch(GUARDIAN, 999L), ErrorCode.CARE_MATCH_NOT_FOUND);
    }

    // 무응답 자동 종료 (배치)

    private static final String NO_RESPONSE = "PCAC0018";

    @Test
    void 수락_대기_무응답은_요청_취소가_되고_양쪽에_알리며_모집글은_그대로다() {
        stubMatch(REQUESTED);
        when(careMatchMapper.existsNoResponseExpired(MATCH, REQUESTED, 14L)).thenReturn(true);

        boolean done = service.expireNoResponse(MATCH, CareMatchStatus.REQUESTED, 14L);

        assertThat(done).isTrue();
        verify(careMatchMapper).updateStatus(MATCH, REQUESTED, CANCELED);
        ArgumentCaptor<CareMatchStatusHistory> history = ArgumentCaptor.forClass(CareMatchStatusHistory.class);
        verify(careMatchMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getMbrSn()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(CANCELED);
        assertThat(history.getValue().getChgRsn()).isEqualTo("14일 무응답 자동 취소");
        verify(chatMessageService).createSystemMessage(ROOM, "14일 동안 수락이 없어 요청이 자동 취소되었어요.");
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(event.capture());
        assertThat(event.getAllValues()).extracting(NotificationEvent::mbrSn).containsExactlyInAnyOrder(GUARDIAN,
                SITTER);
        verify(careRecruitmentMapper, never()).updateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 확인_대기_무응답은_무응답_종료가_되고_모집글은_매칭완료_그대로다() {
        stubMatch(AWAITING_CONFIRM);
        when(careMatchMapper.existsNoResponseExpired(MATCH, AWAITING_CONFIRM, 14L)).thenReturn(true);

        boolean done = service.expireNoResponse(MATCH, CareMatchStatus.AWAITING_CONFIRM, 14L);

        assertThat(done).isTrue();
        verify(careMatchMapper).updateStatus(MATCH, AWAITING_CONFIRM, NO_RESPONSE);
        verify(careMatchMapper, never()).updateStatus(MATCH, AWAITING_CONFIRM, AWAITING_RECEIPT);
        verify(chatMessageService).createSystemMessage(ROOM, "14일 동안 완료 확인이 없어 거래가 무응답 종료되었어요.");
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(any(NotificationEvent.class));
        verify(careRecruitmentMapper, never()).updateStatus(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 잠근_뒤_상태가_바뀌었으면_건너뛴다() {
        stubMatch(MATCHED);

        boolean done = service.expireNoResponse(MATCH, CareMatchStatus.REQUESTED, 14L);

        assertThat(done).isFalse();
        verify(careMatchMapper, never()).updateStatus(anyLong(), anyString(), anyString());
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 잠근_뒤_다시_보니_기준_일수가_안_지났으면_건너뛴다() {
        stubMatch(REQUESTED);
        when(careMatchMapper.existsNoResponseExpired(MATCH, REQUESTED, 14L)).thenReturn(false);

        assertThat(service.expireNoResponse(MATCH, CareMatchStatus.REQUESTED, 14L)).isFalse();
        verify(careMatchMapper, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void 수령_대기는_무응답_처리_대상이_아니다() {
        assertThatThrownBy(() -> service.findNoResponseTargets(CareMatchStatus.AWAITING_RECEIPT, 14L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.expireNoResponse(MATCH, CareMatchStatus.AWAITING_RECEIPT, 14L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
