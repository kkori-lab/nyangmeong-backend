package com.nyangmeong.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.dto.MemberWithdrawRequest;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원탈퇴 서비스]
 * - FN-ACC-020-14, FN-AIM-011-19, FN-MYP-002-08
 * - 회원 행은 지우지 않고 상태만 탈퇴로 바꾼다. 반려동물·건강기록·SNS 연동·암호문은 30일 복구를 위해 그대로 둔다
 * - 상태 변경·이력·세션 폐기가 한 트랜잭션이라 일부만 반영된 상태가 남지 않는다
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawService {

    /** 사용자가 그대로 입력해야 하는 확인 문구 (띄어쓰기·마침표·앞뒤 공백까지 완전 일치) */
    static final String CONFIRM_PHRASE = "정말로 탈퇴하겠습니다.";
    private static final String CHANGE_REASON = "회원 탈퇴";

    private final MemberMapper memberMapper;
    private final LoginSessionManager loginSessionManager;

    /**
     * 탈퇴 실행 : 문구가 다르면 WITHDRAW_PHRASE_MISMATCH, 정상 회원이 아니면(이미 탈퇴·정지) MEMBER_INACTIVE
     */
    @Transactional
    public void withdraw(Long mbrSn, MemberWithdrawRequest req) {
        if (!CONFIRM_PHRASE.equals(req.confirmPhrase())) {
            throw new CustomException(ErrorCode.WITHDRAW_PHRASE_MISMATCH);
        }
        if (memberMapper.updateWithdraw(mbrSn, MemberStatus.NORMAL.getCode(), MemberStatus.WITHDRAWN.getCode(),
                String.valueOf(mbrSn)) != 1) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        memberMapper.insertStatusHistory(mbrSn, MemberStatus.NORMAL.getCode(), MemberStatus.WITHDRAWN.getCode(),
                CHANGE_REASON);
        loginSessionManager.revokeAll(mbrSn);
    }
}
