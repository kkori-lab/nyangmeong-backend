package com.nyangmeong.member.service;

import java.util.List;

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
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.AdminAccountSummary;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원 권한 서비스]
 * - SCR-ADM-113 (FN-ADM-113-01~03)
 * - 권한은 요청마다 DB 값으로 읽어 세션을 폐기하지 않음
 */
@Service
@RequiredArgsConstructor
public class MemberAuthorityService {

    private static final String TARGET_TABLE = "MBR";
    /** 지정일시 조회 기준이라 selectAdminAccountList 와 같은 값 */
    private static final String APPOINT_SUMMARY = "관리자 지정";
    private static final String RELEASE_SUMMARY = "관리자 해제";

    private final MemberMapper memberMapper;
    private final FieldCryptoService fieldCryptoService;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 관리자 목록 조회
     * - 페이지 정보를 잃지 않도록 목록을 새로 만들지 않고 행에 마스킹 값만 채움
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAccountSummary> searchAdmins(PageQuery pq) {
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminAccountSummary> rows = memberMapper.selectAdminAccountList(MemberAuthority.ADMIN.getCode(),
                MemberStatus.WITHDRAWN.getCode(), TARGET_TABLE, APPOINT_SUMMARY);
        rows.forEach(row -> row
                .setMaskedAcntEml(MemberService.maskEmail(fieldCryptoService.decrypt(row.getAcntEmlEnc()))));
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 지정
     */
    @Transactional
    public void appointAdmin(Long actorSn, Long mbrSn) {
        Member member = getMember(mbrSn);
        if (!MemberAuthority.USER.is(member.getMbrAuthrtCd()) || !MemberStatus.NORMAL.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_APPOINTABLE);
        }
        if (memberMapper.updateAuthority(mbrSn, MemberAuthority.ADMIN.getCode(), MemberAuthority.USER.getCode(),
                MemberStatus.NORMAL.getCode(), String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.MEMBER_NOT_APPOINTABLE);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, mbrSn, APPOINT_SUMMARY,
                new AuthorityState(MemberAuthority.USER.getCode()),
                new AuthorityState(MemberAuthority.ADMIN.getCode()));
    }

    /**
     * 관리자 해제
     */
    @Transactional
    public void releaseAdmin(Long actorSn, Long mbrSn) {
        if (mbrSn.equals(actorSn)) {
            throw new CustomException(ErrorCode.ADMIN_NOT_RELEASABLE);
        }
        /** 서로 동시에 해제해 관리자가 0명이 되는 것 방지, 대상 조회보다 먼저 잠가야 커밋된 값을 읽음 */
        List<Long> adminSns = memberMapper.selectNormalAdminSnsForUpdate(MemberAuthority.ADMIN.getCode(),
                MemberStatus.NORMAL.getCode());
        Member member = getMember(mbrSn);
        if (!MemberAuthority.ADMIN.is(member.getMbrAuthrtCd()) || MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        if (adminSns.equals(List.of(mbrSn))) {
            throw new CustomException(ErrorCode.ADMIN_NOT_RELEASABLE);
        }
        if (memberMapper.updateAuthority(mbrSn, MemberAuthority.USER.getCode(), MemberAuthority.ADMIN.getCode(), null,
                String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, mbrSn, RELEASE_SUMMARY,
                new AuthorityState(MemberAuthority.ADMIN.getCode()),
                new AuthorityState(MemberAuthority.USER.getCode()));
    }

    private Member getMember(Long mbrSn) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /**
     * 감사 로그 변경 전후 내용
     */
    private record AuthorityState(String mbrAuthrtCd) {
    }
}
