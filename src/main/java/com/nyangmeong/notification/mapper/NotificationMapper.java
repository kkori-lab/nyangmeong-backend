package com.nyangmeong.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.notification.domain.InappNotification;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.domain.NotificationRecipient;
import com.nyangmeong.notification.domain.NotificationTemplate;
import com.nyangmeong.notification.dto.AdminNotificationTemplateSummary;
import com.nyangmeong.notification.dto.NotificationSummary;

/**
 * [인앱 알림 Mapper]
 */
@Mapper
public interface NotificationMapper {

    /**
     * 회원 알림 수신 설정 조회
     */
    NotificationReceiveSetting selectReceiveSetting(Long mbrSn);

    /**
     * 메일 수신자 조회
     */
    NotificationRecipient selectRecipient(Long mbrSn);

    /**
     * 사용 중인 알림 템플릿 조회
     */
    NotificationTemplate selectTemplate(@Param("notiTypeCd") String notiTypeCd,
            @Param("notiChnlCd") String notiChnlCd);

    /**
     * 관리자 알림 템플릿 목록 조회
     */
    List<AdminNotificationTemplateSummary> selectTemplates(@Param("notiChnlCd") String notiChnlCd);

    /**
     * 번호로 알림 템플릿 조회
     */
    NotificationTemplate selectTemplateBySn(Long notiTmpltSn);

    /**
     * 알림 템플릿 등록
     */
    void insertTemplate(NotificationTemplate template);

    /**
     * 알림 템플릿 수정
     */
    int updateTemplate(NotificationTemplate template);

    /**
     * 회원 알림 수신 설정 수정
     */
    int updateReceiveSetting(@Param("setting") NotificationReceiveSetting setting, @Param("actorId") String actorId);

    /**
     * 인앱 알림 등록
     */
    void insert(InappNotification notification);

    /**
     * 상태가 같고 앱 알림 채널을 켠 회원 전체에 인앱 알림 일괄 등록
     */
    int insertForNormalMembers(@Param("mbrSttsCd") String mbrSttsCd, @Param("notiTypeCd") String notiTypeCd,
            @Param("notiTtl") String notiTtl, @Param("notiCn") String notiCn, @Param("mvmnPath") String mvmnPath);

    /**
     * 미읽음 알림 개수
     */
    long countUnread(Long mbrSn);

    /**
     * 최근 알림 목록 조회
     */
    List<NotificationSummary> selectRecentList(@Param("mbrSn") Long mbrSn,
            @Param("notiTypeCds") List<String> notiTypeCds, @Param("limit") int limit);

    /**
     * 본인 알림 존재 여부
     */
    boolean existsOwn(@Param("inappNotiSn") Long inappNotiSn, @Param("mbrSn") Long mbrSn);

    /**
     * 읽음 처리
     */
    int updateRead(@Param("inappNotiSn") Long inappNotiSn, @Param("mbrSn") Long mbrSn);
}
