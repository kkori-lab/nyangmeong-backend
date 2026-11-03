package com.nyangmeong.community.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.dto.NewsResponse;
import com.nyangmeong.community.dto.NewsSummary;

/**
 * [뉴스 Mapper]
 */
@Mapper
public interface NewsMapper {

    /**
     * 뉴스 목록 조회 (카테고리·제목/내용 검색, 최신순)
     */
    List<NewsSummary> selectList(@Param("newsClsfCd") String newsClsfCd, @Param("keyword") String keyword);

    /**
     * 뉴스 상세 조회
     */
    NewsResponse selectNews(@Param("newsSn") Long newsSn);

    /**
     * 이미 저장된 원문 경로 조회
     */
    List<String> selectOrgtxtPaths(@Param("orgtxtPaths") List<String> orgtxtPaths);

    /**
     * 뉴스 분류 키워드 조회
     */
    List<NewsKeyword> selectKeywords();

    /**
     * 뉴스 제외 키워드 조회 : 행마다 쉼표로 구분한 키워드
     */
    List<String> selectExcludeKeywords();

    /**
     * 중복 판정용 최근 기사 조회 (숨긴 기사 포함, 제목·요약만)
     */
    List<News> selectRecentNews(@Param("since") LocalDateTime since);

    /**
     * 수집한 뉴스 등록 : 건너뛴 기사는 0
     */
    int insert(News news);
}
