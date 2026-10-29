package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * [보낸 초대 목록 항목]
 * - GET /caregiver-invitations, 이메일은 서비스에서 복호화
 * - rcvrJoined : 발송 때 가입 회원 (인앱 알림) 여부
 */
@Data
public class CaregiverInvitationSummary {
    private Long jntBrupInvtnSn;
    private String invtnTrgtEml;
    private boolean rcvrJoined;
    private List<CaregiverInvitationPetSummary> pets;
    private LocalDateTime regDt;
    private String invtnSttsCd;
    private LocalDateTime invtnExpryDt;
}
