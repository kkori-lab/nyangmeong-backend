package com.nyangmeong.community.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.dto.AdminNewsSummary;
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

    /**
     * 관리자 뉴스 목록 조회 (출처·카테고리 필터, 숨긴 기사 포함, 등록 최신순)
     */
    List<AdminNewsSummary> selectAdminList(@Param("newsSrcCd") String newsSrcCd,
            @Param("newsClsfCd") String newsClsfCd);

    /**
     * 뉴스 한 건 조회 (사용 여부 무관)
     */
    News selectById(@Param("newsSn") Long newsSn);

    /**
     * 관리자 뉴스 등록
     */
    int insertAdmin(News news);

    /**
     * 관리자 뉴스 수정 : null 이 아닌 칸만
     */
    int updateAdmin(News news);

    /**
     * 관리자 분류 키워드 목록 (사용 중, 정렬순)
     */
    List<NewsKeyword> selectAdminKeywords();

    /**
     * 분류 키워드 한 건 조회 (사용 중만)
     */
    NewsKeyword selectKeywordById(@Param("kwrdDictSn") Long kwrdDictSn);

    /**
     * 같은 카테고리에 같은 키워드가 있는지 (쉼표로 이어진 지난 행 포함, 대소문자 무시)
     */
    boolean existsKeyword(@Param("mpngCd") String mpngCd, @Param("kwrdCn") String kwrdCn);

    /**
     * 새 키워드의 정렬 순서 : 그 카테고리 기존 행과 같은 값, 없으면 뉴스 분류 최대 + 10
     */
    Integer selectKeywordSortSeq(@Param("mpngCd") String mpngCd);

    /**
     * 분류 키워드 등록
     */
    int insertKeyword(@Param("keyword") NewsKeyword keyword, @Param("actorId") String actorId);

    /**
     * 분류 키워드 삭제 (DEL_YN = 'Y')
     */
    int deleteKeyword(@Param("kwrdDictSn") Long kwrdDictSn, @Param("actorId") String actorId);
}
