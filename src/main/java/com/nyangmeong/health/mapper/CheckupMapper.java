package com.nyangmeong.health.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.CheckupRecord;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSearchCond;
import com.nyangmeong.health.dto.CheckupSummary;

/**
 * [검진기록 매퍼]
 * - CKUP_RCD, CKUP_RCD_STTS_HSTRY, 삭제·반려동물 이동 시 연결 케어 항목·반복 예방 관리
 */
@Mapper
public interface CheckupMapper {

    /**
     * 병원 일정에 연결된 검진기록 목록 조회
     */
    List<CheckupSummary> selectList(@Param("petSn") Long petSn, @Param("cond") CheckupSearchCond cond,
            @Param("hospitalCd") String hospitalCd, @Param("completedCd") String completedCd);

    /**
     * 검진기록 단건 조회 (완료 상태만)
     */
    CheckupResponse selectDetail(@Param("ckupRcdSn") Long ckupRcdSn, @Param("completedCd") String completedCd);

    /**
     * 같은 반려동물·같은 날짜의 삭제되지 않은 병원 일정 중 가장 먼저 등록된 일정번호
     */
    Long selectHospitalScheduleSn(@Param("petSn") Long petSn, @Param("ymd") LocalDate ymd,
            @Param("hospitalCd") String hospitalCd);

    /**
     * 일정에 연결된 완료 상태 검진기록 존재 여부
     */
    boolean existsLinkedCheckup(@Param("schdlSn") Long schdlSn, @Param("completedCd") String completedCd);

    /**
     * 검진기록 등록
     */
    int insertCheckup(CheckupRecord record);

    /**
     * 검진기록 수정
     */
    int updateCheckup(@Param("record") CheckupRecord record, @Param("actorId") String actorId);

    /**
     * 검진기록 상태 변경
     */
    int updateStatus(@Param("ckupRcdSn") Long ckupRcdSn, @Param("sttsCd") String sttsCd,
            @Param("actorId") String actorId);

    /**
     * 검진기록 상태 이력 등록
     */
    int insertStatusHistory(@Param("ckupRcdSn") Long ckupRcdSn, @Param("mbrSn") Long mbrSn,
            @Param("bfrSttsCd") String bfrSttsCd, @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 검진기록의 처방·반복 예방으로 생긴 케어 항목 삭제 처리 (완료 기록은 유지)
     */
    int deleteLinkedCareItems(@Param("ckupRcdSn") Long ckupRcdSn, @Param("actorId") String actorId);

    /**
     * 검진기록의 반복 예방 관리 삭제 처리
     */
    int deleteLinkedRepeatPreventions(@Param("ckupRcdSn") Long ckupRcdSn, @Param("actorId") String actorId);

    /**
     * 검진기록의 케어 항목 반려동물 이동 (메인 표시 해제)
     */
    int updateLinkedCareItemsPet(@Param("ckupRcdSn") Long ckupRcdSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);

    /**
     * 검진기록의 반복 예방 관리 반려동물 이동
     */
    int updateLinkedRepeatPreventionsPet(@Param("ckupRcdSn") Long ckupRcdSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);

    /**
     * 반려동물의 검진기록 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
