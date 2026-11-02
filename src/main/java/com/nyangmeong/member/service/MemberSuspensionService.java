package com.nyangmeong.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.domain.SuspensionType;
import com.nyangmeong.member.dto.AdminMemberReleaseRequest;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.dto.AdminSuspensionSearchCond;
import com.nyangmeong.member.dto.AdminSuspensionSummary;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원 정지 서비스]
 * - SCR-ADM-012 (FN-ADM-012-01~05)
 */
@Service
@RequiredArgsConstructor
public class MemberSuspensionService {

    private static final String TARGET_TABLE = "MBR";
    private static final String EXPIRY_REASON = "정지 기간 만료";
    private static final String SYSTEM_ACTOR = "SYSTEM";
    /** 기간 정지 선택지 */
    private static final Set<Integer> SUSPENSION_DAY_CNTS = Set.of(7, 30);

    private final MemberMapper memberMapper;
    private final AdminTaskLogService adminTaskLogService;
    private final LoginSessionManager loginSessionManager;

    /**
     * 정지 회원 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminSuspensionSummary> searchSuspensions(AdminSuspensionSearchCond cond, PageQuery pq) {
        String q = cond.getQ();
        String mbrNcnm = q != null && !q.isBlank() ? q.strip() : null;
        Boolean permanent = cond.getType() != null ? cond.getType() == SuspensionType.PERMANENT : null;

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminSuspensionSummary> rows = memberMapper.selectSuspensionList(MemberStatus.SUSPENDED.getCode(),
                mbrNcnm, permanent);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 회원 정지
     */
    @Transactional
    public void suspend(Long actorSn, Long mbrSn, AdminMemberSuspendRequest req) {
        if (req.suspnDayCnt() != null && !SUSPENSION_DAY_CNTS.contains(req.suspnDayCnt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Member member = getMember(mbrSn);
        if (mbrSn.equals(actorSn) || !MemberAuthority.USER.is(member.getMbrAuthrtCd())
                || !MemberStatus.NORMAL.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_SUSPENDABLE);
        }

        LocalDateTime suspnEndDt = calculateEndDt(req.suspnDayCnt());
        if (memberMapper.updateSuspend(mbrSn, MemberStatus.SUSPENDED.getCode(), MemberStatus.NORMAL.getCode(),
                suspnEndDt, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.MEMBER_NOT_SUSPENDABLE);
        }
        memberMapper.insertProcessedStatusHistory(mbrSn, actorSn, MemberStatus.NORMAL.getCode(),
                MemberStatus.SUSPENDED.getCode(), req.suspnRsn());
        /** 남겨 두면 해제 뒤 옛 세션이 되살아남 */
        loginSessionManager.revokeAll(mbrSn);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, mbrSn, "회원 정지",
                new SuspensionState(MemberStatus.NORMAL.getCode(), null, null),
                new SuspensionState(MemberStatus.SUSPENDED.getCode(), suspnEndDt, req.suspnRsn()));
    }

    /**
     * 회원 정지 해제
     */
    @Transactional
    public void release(Long actorSn, Long mbrSn, AdminMemberReleaseRequest req) {
        Member member = getMember(mbrSn);
        if (!MemberStatus.SUSPENDED.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        if (memberMapper.updateRelease(mbrSn, MemberStatus.NORMAL.getCode(), MemberStatus.SUSPENDED.getCode(), null,
                String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        memberMapper.insertProcessedStatusHistory(mbrSn, actorSn, MemberStatus.SUSPENDED.getCode(),
                MemberStatus.NORMAL.getCode(), req.rlsRsn());
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, mbrSn, "회원 정지 해제",
                new SuspensionState(MemberStatus.SUSPENDED.getCode(), member.getSuspnEndDt(), null),
                new SuspensionState(MemberStatus.NORMAL.getCode(), null, req.rlsRsn()));
    }

    /**
     * 정지 기간이 끝난 회원 해제
     */
    @Transactional
    public int releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        int released = 0;
        for (Long mbrSn : memberMapper.selectExpiredSuspendedMbrSns(MemberStatus.SUSPENDED.getCode(), now)) {
            /** 조회와 해제 사이에 관리자가 처리한 회원 제외 */
            if (memberMapper.updateRelease(mbrSn, MemberStatus.NORMAL.getCode(), MemberStatus.SUSPENDED.getCode(), now,
                    SYSTEM_ACTOR) != 1) {
                continue;
            }
            memberMapper.insertStatusHistory(mbrSn, MemberStatus.SUSPENDED.getCode(), MemberStatus.NORMAL.getCode(),
                    EXPIRY_REASON);
            released++;
        }
        return released;
    }

    private Member getMember(Long mbrSn) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /**
     * 정지 종료 일시 계산 (영구 정지는 null)
     */
    private LocalDateTime calculateEndDt(Integer suspnDayCnt) {
        return suspnDayCnt != null ? LocalDate.now().plusDays(suspnDayCnt).atStartOfDay() : null;
    }

    /**
     * 감사 로그 변경 전후 내용
     */
    private record SuspensionState(String mbrSttsCd, LocalDateTime suspnEndDt, String chgRsn) {
    }
}
