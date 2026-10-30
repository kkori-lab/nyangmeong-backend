package com.nyangmeong.health.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.dto.PrescriptionSearchCond;
import com.nyangmeong.health.dto.PrescriptionSummary;

/**
 * [처방 매퍼]
 * - PRSC, PRSC_STTS_HSTRY
 */
@Mapper
public interface PrescriptionMapper {

    /**
     * 처방약 목록 조회
     */
    List<PrescriptionSummary> selectList(@Param("cond") PrescriptionSearchCond cond,
            @Param("takingCd") String takingCd);

    /**
     * 검진기록들의 처방약 목록 조회
     */
    List<PrescriptionSummary> selectListByCheckups(@Param("ckupRcdSns") List<Long> ckupRcdSns,
            @Param("cond") PrescriptionSearchCond cond, @Param("takingCd") String takingCd);

    /**
     * 처방약 잠금 조회
     */
    Prescription selectPrescriptionForUpdate(Long prscSn);

    /**
     * 복용 종료일이 지난 복용중 처방약 조회
     */
    List<Prescription> selectExpired(@Param("today") LocalDate today, @Param("takingCd") String takingCd);

    /**
     * 처방약 등록
     */
    int insertPrescription(Prescription prescription);

    /**
     * 처방약 수정
     */
    int updatePrescription(@Param("prescription") Prescription prescription, @Param("actorId") String actorId);

    /**
     * 처방약 삭제
     */
    int deletePrescription(@Param("prscSn") Long prscSn, @Param("actorId") String actorId);

    /**
     * 처방 상태 이력 등록
     */
    int insertStatusHistory(@Param("prscSn") Long prscSn, @Param("mbrSn") Long mbrSn,
            @Param("bfrSttsCd") String bfrSttsCd, @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 반려동물의 처방약 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
