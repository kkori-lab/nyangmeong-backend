package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.CareCompletionRequestCreateRequest;
import com.nyangmeong.community.dto.CareCompletionRequestResponse;
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

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 매칭·거래 서비스]
 * - SCR-CMU-005 매칭 요청 -> 수락 -> 돌봄 완료 요청 -> 완료 확인 -> 수령 확인 순서만 허용, 역할과 현재 상태를 서버에서 다시 검증
 * - 잠금 순서는 모집 행 -> 채팅방 행 -> 매칭 행 : 같은 글의 방들이 동시에 수락해도 한 방만 확정
 * - 단계가 바뀔 때마다 상태 이력·시스템 메시지를 남기고, 상대가 그 방을 보는 중이 아니면 알림을 발행
 */
@Service
@RequiredArgsConstructor
public class CareMatchService {

    private static final String NOTI_TTL = "펫돌봄 거래";
    private static final String CANCEL_BY_LEAVE = "채팅방 나가기";
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

        CareRecruitmentDetail post = careRecruitmentMapper.selectDetailByPstSn(room.getPstSn(),
                PostStatus.NORMAL.getCode());
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
     * 채팅방 나가기에 따른 진행 중 매칭 취소 (매칭 완료 이후였으면 모집글을 모집중으로 되돌림)
     */
    @Transactional
    public void cancelOnLeave(Long mbrSn, ChatRoomDetail room) {
        if (room.getPetPrtcMtchSn() == null) {
            return;
        }
        CareRecruitment recruitment = lockRecruitment(room.getPetPrtcRcrtSn());
        chatRoomMapper.selectRoomForUpdate(room.getOtoChatRoomSn());
        CareMatch match = careMatchMapper.selectByIdForUpdate(room.getPetPrtcMtchSn());
        if (match == null || !CareMatchStatus.isInProgress(match.getMtchDlngSttsCd())) {
            return;
        }

        boolean confirmed = CareMatchStatus.isConfirmed(match.getMtchDlngSttsCd());
        changeStatus(match, CareMatchStatus.CANCELED, mbrSn, CANCEL_BY_LEAVE);
        if (confirmed) {
            changeRecruitmentStatus(recruitment, CareRecruitmentStatus.MATCHED, CareRecruitmentStatus.RECRUITING,
                    null, mbrSn, CANCEL_BY_LEAVE);
        }
        chatMessageService.createSystemMessage(room.getOtoChatRoomSn(), "상대가 채팅방을 나가 매칭이 취소되었어요.");
        Long counterpartMbrSn = mbrSn.equals(room.getOwnerMbrSn()) ? room.getCtptMbrSn() : room.getOwnerMbrSn();
        notifyReceiver(counterpartMbrSn, room.getOtoChatRoomSn(), "상대가 채팅방을 나가 매칭이 취소되었어요.");
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
        }
    }

    private void notifyReceiver(Long receiverMbrSn, Long otoChatRoomSn, String content) {
        if (chatViewRegistry.isViewing(receiverMbrSn, otoChatRoomSn)) {
            return;
        }
        eventPublisher.publish(new NotificationEvent(receiverMbrSn, NotificationType.CARE_MATCH, NOTI_TTL, content,
                null));
    }

    private CareMatchResponse response(CareMatch match) {
        CareMatchCondition condition = careMatchMapper.selectLatestCondition(match.getPetPrtcMtchSn());
        return CareMatchResponse.of(match, condition, careMatchMapper.selectCompletionRequests(match.getPetPrtcMtchSn()));
    }
}
