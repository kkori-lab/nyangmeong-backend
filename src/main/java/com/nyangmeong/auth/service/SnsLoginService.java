package com.nyangmeong.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.auth.dto.SnsLoginRequest;
import com.nyangmeong.auth.dto.SnsLoginResult;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsOAuthClient;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.external.SnsProvider;

import lombok.RequiredArgsConstructor;

/**
 * [SNS 로그인 서비스]
 * - 제공자 호출(최대 두 번, 각 제한 시간까지)은 DB 연결을 잡지 않도록 트랜잭션 밖에서 한다
 * - 받은 회원 정보로 하는 로그인·가입 분기는 SnsLoginProcessor 의 트랜잭션에서 한다
 */
@Service
@RequiredArgsConstructor
public class SnsLoginService {

    private final SnsOAuthClient snsOAuthClient;
    private final SnsLoginProcessor snsLoginProcessor;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SnsLoginResult login(SnsLoginRequest req) {
        SnsProvider provider = SnsProvider.fromCode(req.snsOffrCd());
        if (provider == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        SnsProfile profile = snsOAuthClient.fetchProfile(provider, req.code(), req.state());
        return snsLoginProcessor.process(profile);
    }
}
