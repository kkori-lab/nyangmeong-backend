package com.nyangmeong.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsOAuthClient;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberSnsLinkRequest;

import lombok.RequiredArgsConstructor;

/**
 * [SNS 계정 연동 서비스]
 * - 제공자 호출(최대 두 번, 각 제한 시간까지)은 DB 연결을 잡지 않도록 트랜잭션 밖에서 한다
 * - 받은 계정 ID를 로그인 회원에 연결하는 판단·저장은 MemberService.linkSns 의 트랜잭션에서 한다
 * - 제공자 인증 실패(SNS_AUTH_FAILED)는 로그인 문구가 나가지 않게 연동용 SNS_LINK_FAILED 로 바꾼다
 */
@Service
@RequiredArgsConstructor
public class MemberSnsLinkService {

    private final SnsOAuthClient snsOAuthClient;
    private final MemberService memberService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MemberResponse link(Long mbrSn, MemberSnsLinkRequest req) {
        return link(mbrSn, req, null);
    }

    /**
     * requestOrigin : 브라우저 요청의 Origin 헤더. 토큰 교환의 redirect_uri 를 프론트가 쓴 주소에 맞춘다
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MemberResponse link(Long mbrSn, MemberSnsLinkRequest req, String requestOrigin) {
        SnsProvider provider = SnsProvider.fromCode(req.snsOffrCd());
        if (provider == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        SnsProfile profile;
        try {
            profile = snsOAuthClient.fetchProfile(provider, req.code(), req.state(), requestOrigin);
        } catch (BaseException e) {
            if (e.getErrorCode() == ErrorCode.SNS_AUTH_FAILED) {
                throw new CustomException(ErrorCode.SNS_LINK_FAILED);
            }
            throw e;
        }
        return memberService.linkSns(mbrSn, provider.getCode(), profile.accountId());
    }
}
