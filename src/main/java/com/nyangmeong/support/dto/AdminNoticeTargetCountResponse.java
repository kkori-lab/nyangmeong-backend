package com.nyangmeong.support.dto;

/**
 * [공지 알림 대상 수 응답]
 * - GET /api/admin/notices/notification-target-count (FN-ADM-040-03)
 * - trgtCnt : 정상 회원 수, 발송 확인 모달 "회원 N명에게 알림을 보내요"
 */
public record AdminNoticeTargetCountResponse(int trgtCnt) {
}
