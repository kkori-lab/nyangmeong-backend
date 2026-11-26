package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.CareCancelDemand;
import com.nyangmeong.community.domain.CareCancelDemandStatus;
import com.nyangmeong.community.domain.CareCompletionRequest;
import com.nyangmeong.community.domain.CareCompletionResult;
import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.CareMatchCondition;
import com.nyangmeong.community.domain.CareMatchStatus;
import com.nyangmeong.community.domain.CareMatchStatusHistory;
import com.nyangmeong.community.domain.CareRecruitment;
import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentStatus;
import com.nyangmeong.community.domain.CareRecruitmentStatusHistory;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.ChatRoomStatus;
import com.nyangmeong.community.domain.ChatStatusHistory;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.CareCancelDemandItem;
import com.nyangmeong.community.dto.CareCompletionRequestCreateRequest;
import com.nyangmeong.community.dto.CareCompletionRequestResponse;
import com.nyangmeong.community.dto.CareMatchCancelRequest;
import com.nyangmeong.community.dto.CareDealCancelRequest;
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

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 매칭·거래 서비스]
 * - SCR-CMU-005 매칭 요청 -> 수락 -> 돌봄 완료 요청 -> 완료 확인 -> 수령 확인 순서만 허용, 역할과 현재 상태를 서버에서 다시 검증
 * - 잠금 순서는 모집 행 -> 채팅방 행 -> 매칭 행 : 같은 글의 방들이 동시에 수락해도 한 방만 확정
 * - 확정 이후(매칭 완료~수령 대기)에는 나갈 수 없고, 사유와 함께 거래 취소를 요청해 상대가 동의하면 매칭이 요청 취소로 끝난다
 * - 단계가 바뀔 때마다 상태 이력·시스템 메시지를 남기고, 상대가 그 방을 보는 중이 아니면 알림을 발행
 */
@Service
@RequiredArgsConstructor
public class CareMatchService {

    private static final String NOTI_TTL = "펫돌봄 거래";
    /** 거래 알림 이동 경로 : 프론트 알림 드롭다운이 쿼리의 방 번호로 채팅 모달을 연다 */
    static final String CHAT_ROOM_PATH = "/chat?otoChatRoomSn=%d";
    private static final String CANCEL_BY_LEAVE = "채팅방 나가기";
    private static final String CLOSE_BY_DEAL_CANCEL = "거래 취소 동의";
    /** 거래 취소 요청을 받은 쪽이 이 일수 동안 응답하지 않으면 자동 동의 (사람 결정 2026-11-19, DB·운영 설정 아닌 코드 상수) */
    static final long DEAL_CANCEL_AUTO_APPROVE_DAYS = 7;
    private static final String CLOSE_BY_DEAL_CANCEL_AUTO = "거래 취소 요청 " + DEAL_CANCEL_AUTO_APPROVE_DAYS + "일 무응답 자동 동의";
    private static final String CLOSE_BY_OTHER_MATCH = "다른 분과 매칭되어 모집 종료";
    private static final String REOPEN_BY_CANCEL = "확정 매칭 취소로 모집 재개";
    private static final String CANCEL_BY_OTHER_MATCH = "다른 분과 매칭되어 요청이 취소되었어요.";

    private final CareMatchMapper careMatchMapper;
    private final CareRecruitmentMapper careRecruitmentMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatRoomGuard chatRoomGuard;
    private final ChatMessageService chatMessageService;
    private final ChatViewRegistry chatViewRegistry;
    private final EventPublisher eventPublisher;

    /** 잠금을 잡은 뒤의 거래 문맥 */
    private record Locked(ChatRoomDetail room, CareRecruitment recruitment, CareMatch match, boolean guardian) {

        Long counterpartMbrSn() {
            return guardian ? room.getCtptMbrSn() : room.getOwnerMbrSn();
        }
    }

    /**
     * 매칭 조회
     */
    @Transactional(readOnly = true)
    public CareMatchResponse getMatch(Long mbrSn, Long petPrtcMtchSn) {
        CareMatch match = getMatchOrThrow(petPrtcMtchSn);
        chatRoomGuard.getRoom(mbrSn, match.getOtoChatRoomSn());
        return response(match);
    }

    /**
     * 매칭 요청 (보호자, 취소된 매칭은 같은 행으로 다시 요청)
     * - 상대(지원자)가 나간 방이면 409. 메시지를 보내 상대가 돌아온 뒤 요청할 수 있다
     */
    @Transactional
    public CareMatchResponse request(Long mbrSn, CareMatchCreateRequest req) {
        ChatRoomDetail room = chatRoomGuard.getRoom(mbrSn, req.otoChatRoomSn());
        chatRoomGuard.checkJoined(mbrSn, req.otoChatRoomSn());
        if (!chatRoomGuard.isGuardian(room, mbrSn)) {
            throw new CustomException(ErrorCode.CARE_MATCH_ROLE_DENIED);
        }

        CareRecruitment recruitment = lockRecruitment(room.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(room.getOtoChatRoomSn());

        CareMatch replay = careMatchMapper.selectByIdmptDmndIdForShare(req.idmptDmndId());
        if (replay != null) {
            if (!replay.getOtoChatRoomSn().equals(room.getOtoChatRoomSn())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return response(replay);
        }
        ChatParticipant counterpart = chatRoomMapper.selectParticipant(room.getOtoChatRoomSn(), room.getCtptMbrSn());
        if (counterpart != null && ChatParticipantStatus.LEFT.is(counterpart.getPtcpSttsCd())) {
            throw new CustomException(ErrorCode.CARE_MATCH_COUNTERPART_LEFT);
        }

        CareRecruitmentDetail post = careRecruitmentMapper.selectDetailByPstSn(room.getPstSn(),
                PostStatus.NORMAL.getCode(), ChatParticipantStatus.JOINED.getCode());
        if (post == null || !CareRecruitmentStatus.RECRUITING.is(recruitment.getRcrtSttsCd())) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_CLOSED);
        }

        CareMatch match = careMatchMapper.selectByRoomForUpdate(room.getOtoChatRoomSn());
        String bfrSttsCd = null;
        if (match == null) {
            match = new CareMatch();
            match.setPetPrtcRcrtSn(recruitment.getPetPrtcRcrtSn());
            match.setOtoChatRoomSn(room.getOtoChatRoomSn());
            match.setMtchDlngSttsCd(CareMatchStatus.REQUESTED.getCode());
            match.setIdmptDmndId(req.idmptDmndId());
            careMatchMapper.insertMatch(match);
        } else if (CareMatchStatus.CANCELED.is(match.getMtchDlngSttsCd())) {
            bfrSttsCd = match.getMtchDlngSttsCd();
            int rows = careMatchMapper.updateRequestAgain(match.getPetPrtcMtchSn(), req.idmptDmndId(),
                    CareMatchStatus.CANCELED.getCode(), CareMatchStatus.REQUESTED.getCode());
            if (rows == 0) {
                throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
            }
            match.setMtchDlngSttsCd(CareMatchStatus.REQUESTED.getCode());
        } else {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }

        CareMatchCondition condition = insertCondition(mbrSn, match.getPetPrtcMtchSn(), recruitment, post);
        insertHistory(match.getPetPrtcMtchSn(), mbrSn, condition.getMtchCndVerSn(), bfrSttsCd,
                CareMatchStatus.REQUESTED, null);
        chatMessageService.createSystemMessage(room.getOtoChatRoomSn(), "매칭을 요청했어요.");
        notifyReceiver(room.getCtptMbrSn(), room.getOtoChatRoomSn(), "매칭 요청이 왔어요. 조건을 확인하고 수락해 주세요.");
        return response(match);
    }

    /**
     * 수락 전 매칭 요청 취소 (보호자)
     */
    @Transactional
    public void cancel(Long mbrSn, Long petPrtcMtchSn, CareMatchCancelRequest req) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkRole(locked, true);
        checkStatus(locked.match(), CareMatchStatus.REQUESTED);

        changeStatus(locked.match(), CareMatchStatus.CANCELED, mbrSn, req.cnclRsn().strip());
        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(),
                "요청을 취소했어요. 사유: " + req.cnclRsn().strip());
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "매칭 요청이 취소되었어요.");
    }

    /**
     * 매칭 수락 (제공자, 게시글당 한 제공자만 확정)
     */
    @Transactional
    public void accept(Long mbrSn, Long petPrtcMtchSn) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkRole(locked, false);
        checkStatus(locked.match(), CareMatchStatus.REQUESTED);
        if (!CareRecruitmentStatus.RECRUITING.is(locked.recruitment().getRcrtSttsCd())) {
            throw new CustomException(ErrorCode.CARE_MATCH_ALREADY_MATCHED);
        }

        changeStatus(locked.match(), CareMatchStatus.MATCHED, mbrSn, null);
        changeRecruitmentStatus(locked.recruitment(), CareRecruitmentStatus.RECRUITING,
                CareRecruitmentStatus.MATCHED, locked.match().getPetPrtcMtchSn(), mbrSn, "매칭 수락");
        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "매칭이 완료되었어요.");
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "매칭이 수락되었어요.");
        closeOtherRooms(locked, mbrSn);
    }

    /**
     * 돌봄 완료 요청 (제공자)
     */
    @Transactional
    public CareCompletionRequestResponse requestCompletion(Long mbrSn, Long petPrtcMtchSn,
            CareCompletionRequestCreateRequest req) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkRole(locked, false);
        checkStatus(locked.match(), CareMatchStatus.MATCHED);
        checkNoCancelDemand(petPrtcMtchSn);

        CareMatchCondition condition = careMatchMapper.selectLatestCondition(petPrtcMtchSn);
        /** 별도 입력이 없는 요청이라 NOT NULL 컬럼은 빈 내용·요청 시각·확인 처리로 채움 */
        CareCompletionRequest cycle = new CareCompletionRequest();
        cycle.setPetPrtcMtchSn(petPrtcMtchSn);
        cycle.setMtchCndVerSn(condition.getMtchCndVerSn());
        cycle.setMbrSn(mbrSn);
        cycle.setDmndCycl(careMatchMapper.selectNextDmndCycl(petPrtcMtchSn));
        cycle.setCmptnCn("");
        cycle.setIdmptDmndId(req.idmptDmndId());
        cycle.setPrtcEndDt(LocalDateTime.now());
        cycle.setHnovCmptnIdntyYn("Y");
        careMatchMapper.insertCompletionRequest(cycle);

        changeStatus(locked.match(), CareMatchStatus.AWAITING_CONFIRM, mbrSn, null);
        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "돌봄 완료를 요청했어요.");
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "돌봄 완료 확인을 요청했어요.");
        return CareCompletionRequestResponse.from(cycle);
    }

    /**
     * 돌봄 완료 확인 (보호자)
     */
    @Transactional
    public void confirmCompletion(Long mbrSn, Long petPrtcMtchSn) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkRole(locked, true);
        checkStatus(locked.match(), CareMatchStatus.AWAITING_CONFIRM);
        checkNoCancelDemand(petPrtcMtchSn);

        CareCompletionRequest cycle = careMatchMapper.selectLatestCompletionRequestForUpdate(petPrtcMtchSn);
        if (cycle == null) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
        careMatchMapper.updateCompletionConfirm(cycle.getPrtcCmptnDmndCyclSn(),
                CareCompletionResult.CONFIRMED.getCode());

        changeStatus(locked.match(), CareMatchStatus.AWAITING_RECEIPT, mbrSn, null);
        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "돌봄 완료를 확인했어요.");
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "돌봄 완료가 확인되었어요. 비용을 받으면 수령을 확인해 주세요.");
    }

    /**
     * 비용 수령 확인 (제공자, 받았으면 거래 완료)
     */
    @Transactional
    public void confirmReceipt(Long mbrSn, Long petPrtcMtchSn, CareMatchReceiptConfirmRequest req) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkRole(locked, false);
        checkStatus(locked.match(), CareMatchStatus.AWAITING_RECEIPT);
        checkNoCancelDemand(petPrtcMtchSn);

        if (!Boolean.TRUE.equals(req.rcvYn())) {
            careMatchMapper.updateReceipt(petPrtcMtchSn, "N");
            chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "비용을 아직 받지 못했다고 확인했어요.");
            return;
        }

        careMatchMapper.updateReceipt(petPrtcMtchSn, "Y");
        changeStatus(locked.match(), CareMatchStatus.COMPLETED, mbrSn, null);
        changeRecruitmentStatus(locked.recruitment(), CareRecruitmentStatus.MATCHED,
                CareRecruitmentStatus.COMPLETED, petPrtcMtchSn, mbrSn, "거래 완료");
        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "거래가 완료되었어요.");
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "거래가 완료되었어요.");
    }

    /**
     * 채팅방 나가기에 따른 수락 대기 매칭 취소
     * - 확정 이후(매칭 완료~수령 대기)에는 나갈 수 없다(거래 취소 요청이 동의돼 매칭이 끝난 뒤에만 나갈 수 있음)
     *
     * @return 매칭을 취소하고 안내 메시지를 남겼으면 true
     */
    @Transactional
    public boolean cancelOnLeave(Long mbrSn, ChatRoomDetail room) {
        if (room.getPetPrtcMtchSn() == null) {
            return false;
        }
        lockRecruitment(room.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(room.getOtoChatRoomSn());
        CareMatch match = careMatchMapper.selectByIdForUpdate(room.getPetPrtcMtchSn());
        if (match == null) {
            return false;
        }
        if (CareMatchStatus.isConfirmed(match.getMtchDlngSttsCd())) {
            throw new CustomException(ErrorCode.CHAT_LEAVE_IN_DEAL);
        }
        if (!CareMatchStatus.REQUESTED.is(match.getMtchDlngSttsCd())) {
            return false;
        }

        changeStatus(match, CareMatchStatus.CANCELED, mbrSn, CANCEL_BY_LEAVE);
        chatMessageService.createSystemMessage(room.getOtoChatRoomSn(), "상대가 채팅방을 나가 매칭이 취소되었어요.");
        Long counterpartMbrSn = mbrSn.equals(room.getOwnerMbrSn()) ? room.getCtptMbrSn() : room.getOwnerMbrSn();
        notifyReceiver(counterpartMbrSn, room.getOtoChatRoomSn(), "상대가 채팅방을 나가 매칭이 취소되었어요.");
        return true;
    }

    /**
     * 거래 취소 요청 (매칭 완료~수령 대기, 두 당사자 누구나, 요청중인 건이 없을 때)
     * - 이번 거래에서 내 요청이 거절된 적이 있으면 다시 요청할 수 없다 (상대는 요청 가능, 사람 결정 2026-11-20)
     */
    @Transactional
    public CareCancelDemandItem requestDealCancel(Long mbrSn, Long petPrtcMtchSn, CareDealCancelRequest req) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkConfirmed(locked.match());
        checkNoCancelDemand(petPrtcMtchSn);
        if (careMatchMapper.existsRejectedCancelDemand(petPrtcMtchSn, mbrSn, CareCancelDemandStatus.REJECTED.getCode(),
                CareMatchStatus.MATCHED.getCode())) {
            throw new CustomException(ErrorCode.CARE_DEAL_CANCEL_REJECTED);
        }

        CareCancelDemand demand = new CareCancelDemand();
        demand.setPetPrtcMtchSn(petPrtcMtchSn);
        demand.setDmndMbrSn(mbrSn);
        demand.setCnclRsn(req.cnclRsn().strip());
        demand.setDmndSttsCd(CareCancelDemandStatus.REQUESTING.getCode());
        careMatchMapper.insertCancelDemand(demand);

        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(),
                "거래 취소를 요청했어요. 사유: " + demand.getCnclRsn());
        notifyReceiver(locked.counterpartMbrSn(), locked.room().getOtoChatRoomSn(), "거래 취소 요청이 왔어요.");
        return CareCancelDemandItem.of(demand, mbrSn);
    }

    /**
     * 거래 취소 동의 (요청한 사람의 상대만) : 매칭 요청 취소, 모집글 모집중, 이 방은 읽기 전용
     */
    @Transactional
    public void approveDealCancel(Long mbrSn, Long petPrtcMtchSn) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkConfirmed(locked.match());
        CareCancelDemand demand = getRequestingDemand(petPrtcMtchSn);
        checkCounterpartOfDemander(demand, mbrSn);
        markDemand(demand, CareCancelDemandStatus.APPROVED, mbrSn);

        cancelDeal(locked.room(), locked.recruitment(), locked.match(), mbrSn, demand.getCnclRsn(),
                CLOSE_BY_DEAL_CANCEL, "거래가 취소되었어요.");
        notifyReceiver(demand.getDmndMbrSn(), locked.room().getOtoChatRoomSn(), "거래 취소 요청에 상대가 동의해 거래가 취소되었어요.");
    }

    /**
     * 자동 동의 대상 거래 취소 요청 번호 (요청중, 매칭 완료, 등록 후 7일 이상)
     */
    @Transactional(readOnly = true)
    public List<Long> findDealCancelAutoApproveTargets() {
        return careMatchMapper.selectCancelDemandDueTargets(CareCancelDemandStatus.REQUESTING.getCode(),
                autoApproveStatusCodes(), DEAL_CANCEL_AUTO_APPROVE_DAYS, null);
    }

    /**
     * 거래 취소 요청 무응답 자동 동의 (배치, 처리자 없음) : 직접 동의와 같은 결과
     * - 매칭 완료만 대상 (확인 대기·수령 대기는 돌봄이 끝나 비용 분쟁이 될 수 있어 직접 동의해야 취소)
     * - 잠근 뒤 요청 상태·매칭 상태·경과일을 다시 확인해 그사이 처리된 건은 건너뜀
     *
     * @return 처리했으면 true
     */
    @Transactional
    public boolean autoApproveDealCancel(Long mtchCnclDmndSn) {
        CareCancelDemand peekDemand = careMatchMapper.selectCancelDemandById(mtchCnclDmndSn);
        if (peekDemand == null) {
            return false;
        }
        CareMatch peek = careMatchMapper.selectById(peekDemand.getPetPrtcMtchSn());
        if (peek == null) {
            return false;
        }
        ChatRoomDetail room = chatRoomMapper.selectDetail(peek.getOtoChatRoomSn());
        if (room == null) {
            return false;
        }
        CareRecruitment recruitment = lockRecruitment(peek.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(peek.getOtoChatRoomSn());
        CareMatch match = careMatchMapper.selectByIdForUpdate(peek.getPetPrtcMtchSn());
        if (match == null) {
            return false;
        }
        CareCancelDemand demand = careMatchMapper.selectRequestingCancelDemandForUpdate(match.getPetPrtcMtchSn(),
                CareCancelDemandStatus.REQUESTING.getCode());
        if (demand == null || !demand.getMtchCnclDmndSn().equals(mtchCnclDmndSn)
                || !careMatchMapper.existsCancelDemandDue(mtchCnclDmndSn, CareCancelDemandStatus.REQUESTING.getCode(),
                        autoApproveStatusCodes(), DEAL_CANCEL_AUTO_APPROVE_DAYS, null)) {
            return false;
        }
        markDemand(demand, CareCancelDemandStatus.APPROVED, null);

        String message = DEAL_CANCEL_AUTO_APPROVE_DAYS + "일 동안 응답이 없어 거래 취소 요청에 자동 동의했어요. 거래가 취소되었어요.";
        cancelDeal(room, recruitment, match, null, demand.getCnclRsn(), CLOSE_BY_DEAL_CANCEL_AUTO, message);
        notifyReceiver(room.getOwnerMbrSn(), room.getOtoChatRoomSn(), message);
        notifyReceiver(room.getCtptMbrSn(), room.getOtoChatRoomSn(), message);
        return true;
    }

    /**
     * 자동 동의 하루 전 알림 대상 거래 취소 요청 번호 (등록 후 6일 이상 7일 미만, 하루 한 번 배치라 한 번만 걸림)
     */
    @Transactional(readOnly = true)
    public List<Long> findDealCancelReminderTargets() {
        return careMatchMapper.selectCancelDemandDueTargets(CareCancelDemandStatus.REQUESTING.getCode(),
                autoApproveStatusCodes(), DEAL_CANCEL_AUTO_APPROVE_DAYS - 1, DEAL_CANCEL_AUTO_APPROVE_DAYS);
    }

    /**
     * 자동 동의 하루 전 알림 (배치) : 요청을 받은 쪽(요청자의 상대)에게만, 시스템 메시지는 남기지 않음
     *
     * @return 알렸으면 true
     */
    @Transactional
    public boolean remindDealCancel(Long mtchCnclDmndSn) {
        if (!careMatchMapper.existsCancelDemandDue(mtchCnclDmndSn, CareCancelDemandStatus.REQUESTING.getCode(),
                autoApproveStatusCodes(), DEAL_CANCEL_AUTO_APPROVE_DAYS - 1, DEAL_CANCEL_AUTO_APPROVE_DAYS)) {
            return false;
        }
        CareCancelDemand demand = careMatchMapper.selectCancelDemandById(mtchCnclDmndSn);
        if (demand == null) {
            return false;
        }
        CareMatch match = careMatchMapper.selectById(demand.getPetPrtcMtchSn());
        if (match == null) {
            return false;
        }
        ChatRoomDetail room = chatRoomMapper.selectDetail(match.getOtoChatRoomSn());
        if (room == null) {
            return false;
        }
        Long receiverMbrSn = demand.getDmndMbrSn().equals(room.getOwnerMbrSn()) ? room.getCtptMbrSn()
                : room.getOwnerMbrSn();
        notifyReceiver(receiverMbrSn, room.getOtoChatRoomSn(), "거래 취소 요청에 내일까지 응답하지 않으면 자동으로 동의돼요.");
        return true;
    }

    /**
     * 자동 동의할 수 있는 매칭 상태 (매칭 완료만)
     * - 확인 대기·수령 대기는 돌봄이 끝난 뒤라 비용 분쟁이 될 수 있어 받은 쪽이 직접 동의해야 취소 (사람 결정 2026-11-20)
     */
    private static List<String> autoApproveStatusCodes() {
        return List.of(CareMatchStatus.MATCHED.getCode());
    }

    /** 확정 거래 취소 : 매칭 요청 취소, 모집글 모집중, 같은 글의 닫힌 방 다시 열기, 이 방은 읽기 전용 */
    private void cancelDeal(ChatRoomDetail room, CareRecruitment recruitment, CareMatch match, Long mbrSn,
            String cnclRsn, String closeRsn, String message) {
        changeStatus(match, CareMatchStatus.CANCELED, mbrSn, cnclRsn);
        changeRecruitmentStatus(recruitment, CareRecruitmentStatus.MATCHED, CareRecruitmentStatus.RECRUITING, null,
                mbrSn, closeRsn);
        reopenOtherRooms(room, mbrSn);
        chatMessageService.createSystemMessage(room.getOtoChatRoomSn(), message);
        changeRoomStatus(room.getOtoChatRoomSn(), ChatRoomStatus.ACTIVE, ChatRoomStatus.READ_ONLY, mbrSn, closeRsn);
    }

    /**
     * 거래 취소 거절 (요청한 사람의 상대만) : 거래는 그대로 이어진다
     */
    @Transactional
    public void rejectDealCancel(Long mbrSn, Long petPrtcMtchSn) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkConfirmed(locked.match());
        CareCancelDemand demand = getRequestingDemand(petPrtcMtchSn);
        checkCounterpartOfDemander(demand, mbrSn);
        markDemand(demand, CareCancelDemandStatus.REJECTED, mbrSn);

        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "거래 취소 요청을 거절했어요. 거래를 이어가요.");
        notifyReceiver(demand.getDmndMbrSn(), locked.room().getOtoChatRoomSn(), "거래 취소 요청이 거절되었어요. 거래를 이어가요.");
    }

    /**
     * 거래 취소 요청 철회 (요청한 사람만)
     */
    @Transactional
    public void withdrawDealCancel(Long mbrSn, Long petPrtcMtchSn) {
        Locked locked = lock(mbrSn, petPrtcMtchSn);
        checkConfirmed(locked.match());
        CareCancelDemand demand = getRequestingDemand(petPrtcMtchSn);
        if (!demand.getDmndMbrSn().equals(mbrSn)) {
            throw new CustomException(ErrorCode.CARE_MATCH_ROLE_DENIED);
        }
        markDemand(demand, CareCancelDemandStatus.WITHDRAWN, mbrSn);

        chatMessageService.createSystemMessage(locked.room().getOtoChatRoomSn(), "거래 취소 요청을 철회했어요.");
    }

    /**
     * 채팅방 응답에 실을 거래 취소 요청 (이 매칭의 가장 최근 요청, 없으면 null)
     * - 확정 상태에서는 동의 외의 요청, 요청 취소 상태에서는 동의된 요청만 보여 지난 거래의 요청이 남아 보이지 않게 한다
     */
    @Transactional(readOnly = true)
    public CareCancelDemandItem findCancelDemand(Long mbrSn, ChatRoomDetail room) {
        if (room.getPetPrtcMtchSn() == null) {
            return null;
        }
        CareCancelDemand demand = careMatchMapper.selectLatestCancelDemand(room.getPetPrtcMtchSn());
        if (demand == null) {
            return null;
        }
        String sttsCd = room.getMtchDlngSttsCd();
        boolean approved = CareCancelDemandStatus.APPROVED.is(demand.getDmndSttsCd());
        boolean visible = CareMatchStatus.isConfirmed(sttsCd) ? !approved
                : CareMatchStatus.CANCELED.is(sttsCd) && approved;
        return visible ? CareCancelDemandItem.of(demand, mbrSn) : null;
    }

    /**
     * 무응답 처리 대상 매칭 번호 (FN-CMU-005-06 수락 대기, FN-CMU-005-09 확인 대기)
     */
    @Transactional(readOnly = true)
    public List<Long> findNoResponseTargets(CareMatchStatus from, long days) {
        noResponseTarget(from);
        return careMatchMapper.selectNoResponseTargets(from.getCode(), days);
    }

    /**
     * 기준 일수 동안 응답이 없는 매칭 종료 (배치, 처리자 없음)
     * - 수락 대기 -> 요청 취소 (모집글은 모집중 그대로), 확인 대기 -> 무응답 종료 (모집글은 매칭완료 그대로, 자동 완료는 하지 않음)
     * - 잠근 뒤 상태·경과일을 다시 확인해 그사이 수락·취소·완료 확인된 건은 건너뜀
     *
     * @return 처리했으면 true
     */
    @Transactional
    public boolean expireNoResponse(Long petPrtcMtchSn, CareMatchStatus from, long days) {
        CareMatchStatus to = noResponseTarget(from);
        CareMatch peek = careMatchMapper.selectById(petPrtcMtchSn);
        if (peek == null) {
            return false;
        }
        ChatRoomDetail room = chatRoomMapper.selectDetail(peek.getOtoChatRoomSn());
        if (room == null) {
            return false;
        }
        lockRecruitment(peek.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(peek.getOtoChatRoomSn());
        CareMatch match = careMatchMapper.selectByIdForUpdate(petPrtcMtchSn);
        if (match == null || !from.is(match.getMtchDlngSttsCd())
                || !careMatchMapper.existsNoResponseExpired(petPrtcMtchSn, from.getCode(), days)) {
            return false;
        }

        String message;
        if (to == CareMatchStatus.CANCELED) {
            changeStatus(match, to, null, days + "일 무응답 자동 취소");
            message = days + "일 동안 수락이 없어 요청이 자동 취소되었어요.";
        } else {
            changeStatus(match, to, null, days + "일 무응답 종료");
            message = days + "일 동안 완료 확인이 없어 거래가 무응답 종료되었어요.";
        }
        chatMessageService.createSystemMessage(room.getOtoChatRoomSn(), message);
        notifyReceiver(room.getOwnerMbrSn(), room.getOtoChatRoomSn(), message);
        notifyReceiver(room.getCtptMbrSn(), room.getOtoChatRoomSn(), message);
        return true;
    }

    /** 무응답 처리할 수 있는 상태와 바뀔 상태 */
    private CareMatchStatus noResponseTarget(CareMatchStatus from) {
        if (from == CareMatchStatus.REQUESTED) {
            return CareMatchStatus.CANCELED;
        }
        if (from == CareMatchStatus.AWAITING_CONFIRM) {
            return CareMatchStatus.NO_RESPONSE;
        }
        throw new IllegalArgumentException("무응답 처리 대상 상태가 아님 : " + from);
    }

    private CareMatch getMatchOrThrow(Long petPrtcMtchSn) {
        CareMatch match = careMatchMapper.selectById(petPrtcMtchSn);
        if (match == null) {
            throw new CustomException(ErrorCode.CARE_MATCH_NOT_FOUND);
        }
        return match;
    }

    private Locked lock(Long mbrSn, Long petPrtcMtchSn) {
        CareMatch peek = getMatchOrThrow(petPrtcMtchSn);
        ChatRoomDetail room = chatRoomGuard.getRoom(mbrSn, peek.getOtoChatRoomSn());
        chatRoomGuard.checkJoined(mbrSn, peek.getOtoChatRoomSn());

        CareRecruitment recruitment = lockRecruitment(peek.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(peek.getOtoChatRoomSn());
        CareMatch match = careMatchMapper.selectByIdForUpdate(petPrtcMtchSn);
        if (match == null) {
            throw new CustomException(ErrorCode.CARE_MATCH_NOT_FOUND);
        }
        return new Locked(room, recruitment, match, chatRoomGuard.isGuardian(room, mbrSn));
    }

    private CareRecruitment lockRecruitment(Long petPrtcRcrtSn) {
        CareRecruitment recruitment = careRecruitmentMapper.selectForUpdate(petPrtcRcrtSn);
        if (recruitment == null) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        return recruitment;
    }

    private void checkRole(Locked locked, boolean guardianOnly) {
        if (locked.guardian() != guardianOnly) {
            throw new CustomException(ErrorCode.CARE_MATCH_ROLE_DENIED);
        }
    }

    /** 매칭 완료~수령 대기가 아니면 거래 취소를 다룰 수 없다 */
    private void checkConfirmed(CareMatch match) {
        if (!CareMatchStatus.isConfirmed(match.getMtchDlngSttsCd())) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
    }

    /** 요청중인 거래 취소가 있으면 거래 단계를 바꿀 수 없다 (매칭 잠금 뒤 호출) */
    private void checkNoCancelDemand(Long petPrtcMtchSn) {
        if (careMatchMapper.selectRequestingCancelDemandForUpdate(petPrtcMtchSn,
                CareCancelDemandStatus.REQUESTING.getCode()) != null) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
    }

    private CareCancelDemand getRequestingDemand(Long petPrtcMtchSn) {
        CareCancelDemand demand = careMatchMapper.selectRequestingCancelDemandForUpdate(petPrtcMtchSn,
                CareCancelDemandStatus.REQUESTING.getCode());
        if (demand == null) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
        return demand;
    }

    private void checkCounterpartOfDemander(CareCancelDemand demand, Long mbrSn) {
        if (demand.getDmndMbrSn().equals(mbrSn)) {
            throw new CustomException(ErrorCode.CARE_MATCH_ROLE_DENIED);
        }
    }

    private void markDemand(CareCancelDemand demand, CareCancelDemandStatus to, Long mbrSn) {
        if (careMatchMapper.updateCancelDemandStatus(demand.getMtchCnclDmndSn(),
                CareCancelDemandStatus.REQUESTING.getCode(), to.getCode(), mbrSn) == 0) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
    }

    private void checkStatus(CareMatch match, CareMatchStatus expected) {
        if (!expected.is(match.getMtchDlngSttsCd())) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
    }

    private void changeStatus(CareMatch match, CareMatchStatus to, Long mbrSn, String chgRsn) {
        String bfrSttsCd = match.getMtchDlngSttsCd();
        int rows = careMatchMapper.updateStatus(match.getPetPrtcMtchSn(), bfrSttsCd, to.getCode());
        if (rows == 0) {
            throw new CustomException(ErrorCode.CARE_MATCH_INVALID_STATE);
        }
        match.setMtchDlngSttsCd(to.getCode());
        CareMatchCondition condition = careMatchMapper.selectLatestCondition(match.getPetPrtcMtchSn());
        insertHistory(match.getPetPrtcMtchSn(), mbrSn, condition == null ? null : condition.getMtchCndVerSn(),
                bfrSttsCd, to, chgRsn);
    }

    private void insertHistory(Long petPrtcMtchSn, Long mbrSn, Long mtchCndVerSn, String bfrSttsCd,
            CareMatchStatus to, String chgRsn) {
        CareMatchStatusHistory history = new CareMatchStatusHistory();
        history.setPetPrtcMtchSn(petPrtcMtchSn);
        history.setMbrSn(mbrSn);
        history.setMtchCndVerSn(mtchCndVerSn);
        history.setBfrSttsCd(bfrSttsCd);
        history.setAftrSttsCd(to.getCode());
        history.setChgRsn(chgRsn);
        careMatchMapper.insertStatusHistory(history);
    }

    private CareMatchCondition insertCondition(Long mbrSn, Long petPrtcMtchSn, CareRecruitment recruitment,
            CareRecruitmentDetail post) {
        CareMatchCondition condition = new CareMatchCondition();
        condition.setPetPrtcMtchSn(petPrtcMtchSn);
        condition.setMbrSn(mbrSn);
        condition.setCstUnitCd(recruitment.getCstUnitCd());
        condition.setCndVerNo(careMatchMapper.selectNextCndVerNo(petPrtcMtchSn));
        condition.setAcrdCstAmt(recruitment.getCstAmt());
        condition.setAcrdPrtcCn(post.getPstCn());
        condition.setAcrdPrtcBgngDt(recruitment.getPrtcBgngDt());
        condition.setAcrdPrtcEndDt(recruitment.getPrtcEndDt());
        condition.setSchdlCnsltnPsbltyYn(recruitment.getSchdlCnsltnPsbltyYn());
        careMatchMapper.insertCondition(condition);
        return condition;
    }

    private void changeRecruitmentStatus(CareRecruitment recruitment, CareRecruitmentStatus from,
            CareRecruitmentStatus to, Long cfmtnPetPrtcMtchSn, Long mbrSn, String chgRsn) {
        int rows = careRecruitmentMapper.updateStatus(recruitment.getPetPrtcRcrtSn(), from.getCode(), to.getCode(),
                cfmtnPetPrtcMtchSn);
        if (rows == 0) {
            throw new CustomException(from == CareRecruitmentStatus.RECRUITING
                    ? ErrorCode.CARE_MATCH_ALREADY_MATCHED
                    : ErrorCode.CARE_MATCH_INVALID_STATE);
        }
        recruitment.setRcrtSttsCd(to.getCode());
        CareRecruitmentStatusHistory history = new CareRecruitmentStatusHistory();
        history.setPetPrtcRcrtSn(recruitment.getPetPrtcRcrtSn());
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(from.getCode());
        history.setAftrSttsCd(to.getCode());
        history.setChgRsn(chgRsn);
        careRecruitmentMapper.insertStatusHistory(history);
    }

    private void closeOtherRooms(Locked locked, Long mbrSn) {
        List<Long> otherRoomSns = chatRoomMapper.selectOtherRoomSns(locked.room().getPstSn(),
                locked.room().getOtoChatRoomSn());
        for (Long otherRoomSn : otherRoomSns) {
            CareMatch other = careMatchMapper.selectByRoomForUpdate(otherRoomSn);
            if (other != null && CareMatchStatus.REQUESTED.is(other.getMtchDlngSttsCd())) {
                changeStatus(other, CareMatchStatus.CANCELED, mbrSn, CANCEL_BY_OTHER_MATCH);
            }
            chatMessageService.createSystemMessage(otherRoomSn, "다른 분과 매칭되어 모집이 종료되었어요.");
            changeRoomStatus(otherRoomSn, ChatRoomStatus.ACTIVE, ChatRoomStatus.READ_ONLY, mbrSn,
                    CLOSE_BY_OTHER_MATCH);
        }
    }

    /** 채팅방 상태를 바꾸고 이력을 남김 (현재 상태가 from 이 아니면 건너뜀) */
    private void changeRoomStatus(Long otoChatRoomSn, ChatRoomStatus from, ChatRoomStatus to, Long mbrSn,
            String chgRsn) {
        if (chatRoomMapper.updateRoomStatus(otoChatRoomSn, from.getCode(), to.getCode()) == 0) {
            return;
        }
        ChatStatusHistory history = new ChatStatusHistory();
        history.setRefSn(otoChatRoomSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(from.getCode());
        history.setAftrSttsCd(to.getCode());
        history.setChgRsn(chgRsn);
        chatRoomMapper.insertRoomStatusHistory(history);
    }

    /** 확정 매칭이 취소돼 모집중으로 돌아가면, 그 글의 읽기 전용 방을 다시 연다 */
    private void reopenOtherRooms(ChatRoomDetail room, Long mbrSn) {
        List<Long> closedRoomSns = chatRoomMapper.selectOtherRoomSnsByStatus(room.getPstSn(),
                room.getOtoChatRoomSn(), ChatRoomStatus.READ_ONLY.getCode());
        for (Long closedRoomSn : closedRoomSns) {
            changeRoomStatus(closedRoomSn, ChatRoomStatus.READ_ONLY, ChatRoomStatus.ACTIVE, mbrSn,
                    REOPEN_BY_CANCEL);
            chatMessageService.createSystemMessage(closedRoomSn, "모집이 다시 열렸어요.");
        }
    }

    private void notifyReceiver(Long receiverMbrSn, Long otoChatRoomSn, String content) {
        if (chatViewRegistry.isViewing(receiverMbrSn, otoChatRoomSn)) {
            return;
        }
        eventPublisher.publish(new NotificationEvent(receiverMbrSn, NotificationType.CARE_MATCH, NOTI_TTL, content,
                String.format(CHAT_ROOM_PATH, otoChatRoomSn)));
    }

    private CareMatchResponse response(CareMatch match) {
        CareMatchCondition condition = careMatchMapper.selectLatestCondition(match.getPetPrtcMtchSn());
        return CareMatchResponse.of(match, condition, careMatchMapper.selectCompletionRequests(match.getPetPrtcMtchSn()));
    }
}
