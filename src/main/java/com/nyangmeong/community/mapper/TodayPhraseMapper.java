package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.nyangmeong.community.domain.TodayPhrase;

/**
 * [오늘의 한마디 후보 문구 Mapper]
 * - TODAY_PHRS
 */
@Mapper
public interface TodayPhraseMapper {

    /**
     * 후보 문구 목록 (사용 안 함 포함, 등록순)
     */
    List<TodayPhrase> selectList();

    /**
     * 번호로 후보 문구 조회
     */
    TodayPhrase selectById(Long todayPhrsSn);

    /**
     * 사용 중 후보 문구 1건 무작위 조회 (없으면 null)
     */
    TodayPhrase selectRandomInUse();

    /**
     * 후보 문구 등록
     */
    int insert(TodayPhrase phrase);

    /**
     * 후보 문구 수정 (null 이 아닌 칸만)
     */
    int update(TodayPhrase phrase);
}
