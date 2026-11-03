package com.nyangmeong.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.notification.domain.NotificationSendingLog;
import com.nyangmeong.notification.dto.AdminNotificationLogSearchCond;
import com.nyangmeong.notification.dto.AdminNotificationLogSummary;

/**
 * [알림 발송 이력 Mapper]
 */
@Mapper
public interface NotificationSendingLogMapper {

    /**
     * 발송 이력 선점 등록
     * - 중복방지키(UK_NOTI_SNDNG_LOG_1)가 이미 있으면 DuplicateKeyException : 같은 알림 재발송 방지
     */
    void insert(NotificationSendingLog sendingLog);

    /**
     * 발송 결과 수정
     */
    int updateResult(@Param("notiSndngLogSn") Long notiSndngLogSn, @Param("sndngRsltCd") String sndngRsltCd,
            @Param("rtryNmtm") int rtryNmtm, @Param("failRsn") String failRsn, @Param("actorId") String actorId);

    /**
     * 관리자 발송 이력 목록 (최신순, PageHelper 페이징)
     * - 알림 본문은 읽지 않는다
     */
    List<AdminNotificationLogSummary> selectAdminList(AdminNotificationLogSearchCond cond);
}
