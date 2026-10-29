package com.nyangmeong.member.dto;

import java.util.List;

/**
 * [비밀번호 찾기 계정 판단 결과]
 * - certRslt : PASSWORD_RESET_READY / SNS_ONLY / MEMBER_NOT_FOUND / MEMBER_WITHDRAWN
 * - snsOffrCds : SNS_ONLY 일 때 실제 연결된 제공자 코드, 그 밖에는 null
 */
public record PasswordResetAccount(String certRslt, List<String> snsOffrCds) {

    public boolean resettable() {
        return EmailCertificationResponse.PASSWORD_RESET_READY.equals(certRslt);
    }
}
