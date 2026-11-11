package com.nyangmeong.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 */
@Service
@RequiredArgsConstructor
public class MemberSnsLinkService {

    private final SnsOAuthClient snsOAuthClient;
    private final MemberService memberService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MemberResponse link(Long mbrSn, MemberSnsLinkRequest req) {
        SnsProvider provider = SnsProvider.fromCode(req.snsOffrCd());
        if (provider == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        SnsProfile profile = snsOAuthClient.fetchProfile(provider, req.code(), req.state());
        return memberService.linkSns(mbrSn, provider.getCode(), profile.accountId());
    }
}
