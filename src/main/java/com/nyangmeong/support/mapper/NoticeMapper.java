package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.domain.Notice;
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
}
