package com.nyangmeong.pet.dto;

import com.nyangmeong.global.error.ErrorCode;

/**
 * [공동양육 초대 발송 처리 결과]
 * - 이미 가족·중복 초대는 예외가 아니라 결과로 돌려줌 : 응답 data 에 반려동물명·만료일시를 싣기 위해
 */
public record CaregiverInvitationCreateResult(ErrorCode error, CaregiverInvitationResponse response,
        CaregiverInvitationConflictResponse conflict) {

    public static CaregiverInvitationCreateResult success(CaregiverInvitationResponse response) {
        return new CaregiverInvitationCreateResult(null, response, null);
    }

    public static CaregiverInvitationCreateResult fail(ErrorCode error, CaregiverInvitationConflictResponse conflict) {
        return new CaregiverInvitationCreateResult(error, null, conflict);
    }

    public boolean succeeded() {
        return error == null;
    }
}
