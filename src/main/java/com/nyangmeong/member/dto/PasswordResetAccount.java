package com.nyangmeong.member.dto;

import java.util.List;

/**
 * [비밀번호 찾기 계정 판단 결과]
 * - certRslt : PASSWORD_RESET_READY / SNS_ONLY / MEMBER_NOT_FOUND / MEMBER_WITHDRAWN
 * - snsOffrCds : SNS_ONLY 일 때 실제 연결된 제공자 코드, 그 밖에는 null
 * - mbrSn : PASSWORD_RESET_READY 일 때만, 그 밖에는 null
 */
public record PasswordResetAccount(String certRslt, List<String> snsOffrCds, Long mbrSn) {

    public PasswordResetAccount(String certRslt, List<String> snsOffrCds) {
        this(certRslt, snsOffrCds, null);
    }

    public boolean resettable() {
        return EmailCertificationResponse.PASSWORD_RESET_READY.equals(certRslt);
    }
}
