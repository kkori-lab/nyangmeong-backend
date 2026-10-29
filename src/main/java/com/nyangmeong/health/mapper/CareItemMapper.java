package com.nyangmeong.health.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.CareCompletion;
import com.nyangmeong.health.domain.CareItem;
import com.nyangmeong.health.dto.CareCompletionSearchCond;
import com.nyangmeong.health.dto.CareCompletionSummary;
import com.nyangmeong.health.dto.CareItemSearchCond;
import com.nyangmeong.health.dto.CareItemSummary;

/**
 * [케어 항목 매퍼]
 * - CARE_ARTCL, CARE_CMPTN_HSTRY
 */
@Mapper
public interface CareItemMapper {

    /**
     * 반려동물 행 잠금 (기본 항목 중복 생성 방지)
     */
    Long selectPetForUpdate(Long petSn);

    /**
     * 반려동물 기본 항목 수 조회
     */
    long countBasicItems(Long petSn);

    /**
     * 반려동물 기본 항목 수 잠금 조회
     */
    long countBasicItemsForUpdate(Long petSn);

    /**
     * 반려동물 케어 항목 중 요청 목록에 든 항목 수 조회
     */
    long countItemsIn(@Param("petSn") Long petSn, @Param("careArtclSns") List<Long> careArtclSns);

    /**
     * 케어 항목 목록 조회
     */
    List<CareItemSummary> selectList(@Param("petSn") Long petSn, @Param("cond") CareItemSearchCond cond,
            @Param("today") LocalDate today, @Param("regularCd") String regularCd);

    /**
     * 여러 반려동물의 케어 항목명 조회
     */
    List<CareItem> selectNamesByPets(@Param("petSns") List<Long> petSns);

    /**
     * 케어 항목 단건 조회 (목록 행 형태)
     */
    CareItemSummary selectSummary(@Param("careArtclSn") Long careArtclSn, @Param("today") LocalDate today);

    /**
     * 케어 항목 잠금 조회
     */
    CareItem selectItemForUpdate(Long careArtclSn);

    /**
     * 케어 항목 등록
     */
    int insertItem(CareItem item);

    /**
     * 케어 항목 수정
     */
    int updateItem(@Param("item") CareItem item, @Param("actorId") String actorId);

    /**
     * 케어 항목 삭제
     */
    int deleteItem(@Param("careArtclSn") Long careArtclSn, @Param("actorId") String actorId);

    /**
     * 반려동물 메인 표시 전체 해제
     */
    int updateMainExposureClear(@Param("petSn") Long petSn, @Param("actorId") String actorId);

    /**
     * 케어 항목 메인 표시 지정
     */
    int updateMainExposure(@Param("careArtclSn") Long careArtclSn, @Param("mainSortSeq") int mainSortSeq,
            @Param("actorId") String actorId);

    /**
     * 케어 완료 기록 목록 조회
     */
    List<CareCompletionSummary> selectCompletionList(@Param("petSn") Long petSn,
            @Param("cond") CareCompletionSearchCond cond);

    /**
     * 케어 완료 기록 단건 조회
     */
    CareCompletion selectCompletion(Long careCmptnHstrySn);

    /**
     * 케어 완료 기록 잠금 조회
     */
    CareCompletion selectCompletionForUpdate(Long careCmptnHstrySn);

    /**
     * 항목의 완료일 기록 잠금 조회 (삭제된 기록 포함)
     */
    CareCompletion selectCompletionByDateForUpdate(@Param("careArtclSn") Long careArtclSn,
            @Param("cmptnYmd") LocalDate cmptnYmd);

    /**
     * 항목의 가장 최근 완료일 조회
     */
    LocalDate selectLatestCompletionYmd(Long careArtclSn);

    /**
     * 케어 완료 기록 등록 (삭제된 같은 날짜 기록은 되살림)
     */
    int upsertCompletion(@Param("careArtclSn") Long careArtclSn, @Param("mbrSn") Long mbrSn,
            @Param("cmptnYmd") LocalDate cmptnYmd, @Param("cmptnCycl") int cmptnCycl);

    /**
     * 케어 완료 회차 수정
     */
    int updateCompletionCycle(@Param("careCmptnHstrySn") Long careCmptnHstrySn, @Param("cmptnCycl") int cmptnCycl,
            @Param("actorId") String actorId);

    /**
     * 케어 완료 기록 삭제
     */
    int deleteCompletion(@Param("careCmptnHstrySn") Long careCmptnHstrySn, @Param("actorId") String actorId);

    /**
     * 처방약 1일 복용 횟수 조회
     */
    Integer selectDayMdcnNmtm(Long prscSn);

    /**
     * 처방약 연동 투약 항목 존재 여부
     */
    boolean existsPrescriptionItem(Long prscSn);

    /**
     * 처방약 연동 투약 항목 삭제
     */
    int deletePrescriptionItems(@Param("prscSn") Long prscSn, @Param("actorId") String actorId);

    /**
     * 반복 예방 연동 케어 항목 삭제
     */
    int deleteRepeatPreventionItems(@Param("reptPrvntMngSn") Long reptPrvntMngSn,
            @Param("actorId") String actorId);

    /**
     * 반복 예방 시작일 조회
     */
    LocalDate selectRepeatPreventionBgngYmd(Long reptPrvntMngSn);
}
