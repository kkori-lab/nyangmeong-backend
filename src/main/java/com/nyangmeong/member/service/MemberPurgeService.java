package com.nyangmeong.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [탈퇴 회원 파기 서비스]
 * - FN-ADM-013-02, FN-ACC-020-01, REQ-NFR-002
 * - MBR 행은 다른 테이블이 참조하고 거래·감사 이력이 영구 보관이라 지우지 않고 식별값만 없앤다. 상태는 탈퇴 그대로
 */
@Service
@RequiredArgsConstructor
public class MemberPurgeService {

    /** 파기 표시 : 16진수 HMAC 에는 나올 수 없는 값 */
    private static final String PURGED_HMAC_PREFIX = "PURGED";
    /** 관리자 회원 목록이 복호화하므로 빈 값 대신 자리표시 값을 암호화 */
    private static final String PURGED_EMAIL = "purged@deleted.invalid";
    /** 닉네임 UK 때문에 회원 번호를 붙인다 */
    private static final String PURGED_NICKNAME_PREFIX = "탈퇴회원#";
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final MemberMapper memberMapper;
    private final SystemSettingService systemSettingService;
    private final FieldCryptoService fieldCryptoService;

    /**
     * 파기 대상 회원 번호 조회 : 보관일수가 지났고 아직 파기하지 않은 탈퇴 회원
     */
    @Transactional(readOnly = true)
    public List<Long> searchTargets() {
        return memberMapper.selectPurgeTargetMbrSns(MemberStatus.WITHDRAWN.getCode(), keepDayCnt(),
                PURGED_HMAC_PREFIX + "%");
    }

    /**
     * 회원 파기 : 조회와 파기 사이에 복구한 회원은 건너뜀
     */
    @Transactional
    public boolean purge(Long mbrSn) {
        String acntEmlHmac = PURGED_HMAC_PREFIX + "%058d".formatted(mbrSn);
        if (memberMapper.updatePurge(mbrSn, MemberStatus.WITHDRAWN.getCode(), keepDayCnt(),
                fieldCryptoService.encrypt(PURGED_EMAIL), acntEmlHmac, PURGED_NICKNAME_PREFIX + mbrSn,
                SYSTEM_ACTOR) != 1) {
            return false;
        }
        memberMapper.deleteSnsLinks(mbrSn);
        return true;
    }

    private int keepDayCnt() {
        return systemSettingService.get().getWhdwlMbrKpngDayCnt().intValue();
    }
}
