package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.domain.Notice;
import com.nyangmeong.support.dto.AdminNoticeSummary;
import com.nyangmeong.support.dto.NoticeSummary;

/**
 * [공지사항 Mapper]
 */
@Mapper
public interface NoticeMapper {

    /**
     * 공지사항 목록 조회 (제목 검색, 최신순이면 고정 무시)
     */
    List<NoticeSummary> selectList(@Param("keyword") String keyword, @Param("latest") boolean latest);

    /**
     * 사용 중인 공지사항 단건 조회
     */
    Notice selectNotice(Long ntcMttrSn);

    /**
     * 관리자 공지 목록 조회 (비노출 포함, 제목 검색)
     */
    List<AdminNoticeSummary> selectAdminList(@Param("keyword") String keyword);

    /**
     * 관리자 공지 단건 조회 (사용 여부 무관)
     */
    Notice selectAdminNotice(Long ntcMttrSn);

    /**
     * 공지 등록
     */
    int insert(Notice notice);

    /**
     * 공지 수정 (null 인 값은 그대로)
     */
    int update(Notice notice);

    /**
     * 공지 알림 발송 기록
     */
    int updateNotificationSent(@Param("ntcMttrSn") Long ntcMttrSn, @Param("notiTrgtCnt") int notiTrgtCnt);
}
