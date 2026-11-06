package com.nyangmeong.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.PasswordChangeRequest;
import com.nyangmeong.member.dto.PasswordVerifyRequest;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [로그인 회원의 비밀번호 확인·변경 서비스]
 * - 본인 확인은 서버에 상태를 남기지 않는다(맞으면 통과, 틀리면 CURRENT_PASSWORD_MISMATCH)
 * - 변경은 현재 비밀번호를 같은 요청에서 다시 검사하고, 성공하면 이 회원의 모든 세션을 끊는다
 *   (요청한 기기에 새 세션을 주는 것은 컨트롤러가 한다)
 */
@Service
@RequiredArgsConstructor
public class MemberPasswordService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionManager loginSessionManager;

    /**
     * 현재 비밀번호 확인 : SNS 전용 회원(해시 없음)은 SNS_ONLY_MEMBER
     */
    @Transactional(readOnly = true)
    public void verify(Long mbrSn, PasswordVerifyRequest req) {
        checkCurrentPassword(mbrSn, req.pswd());
    }

    /**
     * 비밀번호 변경 : 확인값 → 현재 비밀번호 순으로 검사하고, 저장한 뒤 모든 세션을 폐기한다
     * - 바뀐 행이 없으면(그 사이 탈퇴) MEMBER_NOT_FOUND
     */
    @Transactional
    public void change(Long mbrSn, PasswordChangeRequest req) {
        if (!req.pswd().equals(req.pswdCnfrm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        checkCurrentPassword(mbrSn, req.currentPswd());
        /** 현재 비밀번호가 맞다고 확인했으므로 원문이 같으면 바뀌는 것이 없다 */
        if (req.pswd().equals(req.currentPswd())) {
            throw new CustomException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }
        if (memberMapper.updatePassword(mbrSn, passwordEncoder.encode(req.pswd()),
                MemberStatus.WITHDRAWN.getCode(), String.valueOf(mbrSn)) != 1) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        loginSessionManager.revokeAll(mbrSn);
    }

    private void checkCurrentPassword(Long mbrSn, String rawPassword) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (MemberStatus.isInactive(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        if (member.getPswdHash() == null) {
            throw new CustomException(ErrorCode.SNS_ONLY_MEMBER);
        }
        if (!passwordEncoder.matches(rawPassword, member.getPswdHash())) {
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
    }
}
