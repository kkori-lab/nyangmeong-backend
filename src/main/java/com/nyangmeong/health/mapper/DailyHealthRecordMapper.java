package com.nyangmeong.health.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.DailyHealthRecord;
import com.nyangmeong.health.dto.DailyHealthRecordSearchCond;
import com.nyangmeong.health.dto.DailyHealthRecordSummary;

/**
 * [일상 건강기록 매퍼]
 * - DAILY_HLTH_RCD
 */
@Mapper
public interface DailyHealthRecordMapper {

    /**
     * 일상 건강기록 목록 조회(기록 시각 오름차순, size가 있으면 최신 N건)
     */
    List<DailyHealthRecordSummary> selectList(@Param("cond") DailyHealthRecordSearchCond cond);

    /**
     * 일상 건강기록 조회(경로 반려동물 기준)
     */
    DailyHealthRecord selectRecord(@Param("petSn") Long petSn, @Param("dailyHlthRcdSn") Long dailyHlthRcdSn);

    /**
     * 일상 건강기록 응답용 조회(배변상태 이름 포함)
     */
    DailyHealthRecordSummary selectSummary(@Param("dailyHlthRcdSn") Long dailyHlthRcdSn);

    /**
     * 반려동물의 최신 체중 기록 수치 조회(없으면 null)
     */
    BigDecimal selectLatestWeight(@Param("petSn") Long petSn, @Param("weightCd") String weightCd);

    /**
     * 공통코드 그룹의 하위 코드 존재 여부
     */
    boolean existsChildCode(@Param("prntComCd") String prntComCd, @Param("comCd") String comCd);

    /**
     * 일상 건강기록 등록
     */
    int insertRecord(DailyHealthRecord record);

    /**
     * 일상 건강기록 수정
     */
    int updateRecord(@Param("record") DailyHealthRecord record, @Param("actorId") String actorId);

    /**
     * 일상 건강기록 삭제
     */
    int deleteRecord(@Param("dailyHlthRcdSn") Long dailyHlthRcdSn, @Param("actorId") String actorId);

    /**
     * 반려동물의 일상 건강기록 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
