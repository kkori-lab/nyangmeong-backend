package com.nyangmeong.pet.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.InvtnTrgtPet;
import com.nyangmeong.pet.domain.JntBrupInvtn;
import com.nyangmeong.pet.domain.JntBrupInvtnSttsHstry;
import com.nyangmeong.pet.dto.CaregiverInvitationPetSummary;
import com.nyangmeong.pet.dto.CaregiverInvitationSummary;

/**
 * [공동양육 초대 Mapper]
 * - JNT_BRUP_INVTN·INVTN_TRGT_PET·JNT_BRUP_INVTN_STTS_HSTRY 조회·저장 (SCR-MYP-005)
 * - 만료는 대기중 + 만료일시 경과로 계산 : 상태 코드와 기준 시각을 서비스가 넘김
 */
@Mapper
public interface CaregiverInvitationMapper {

    /**
     * 초대 단건 조회
     */
    JntBrupInvtn selectInvitation(@Param("jntBrupInvtnSn") Long jntBrupInvtnSn);

    /**
     * 내가 보낸 초대 목록 조회
     */
    List<CaregiverInvitationSummary> selectSentList(@Param("sndrMbrSn") Long sndrMbrSn,
            @Param("pendingCd") String pendingCd, @Param("acceptedCd") String acceptedCd,
            @Param("rejectedCd") String rejectedCd, @Param("expiredCd") String expiredCd,
            @Param("now") LocalDateTime now);

    /**
     * 초대 대상 반려동물 조회
     */
    List<CaregiverInvitationPetSummary> selectInvitationPets(@Param("jntBrupInvtnSn") Long jntBrupInvtnSn,
            @Param("pendingCd") String pendingCd, @Param("acceptedCd") String acceptedCd,
            @Param("rejectedCd") String rejectedCd, @Param("expiredCd") String expiredCd,
            @Param("now") LocalDateTime now);

    /**
     * 초대 대상 반려동물 번호 조회
     */
    List<Long> selectTargetPetSns(@Param("jntBrupInvtnSn") Long jntBrupInvtnSn);

    /**
     * 같은 이메일·반려동물의 만료 전 대기 초대 만료일시
     */
    LocalDateTime selectPendingExpryDt(@Param("invtnTrgtEmlHmac") String invtnTrgtEmlHmac,
            @Param("petSn") Long petSn, @Param("pendingCd") String pendingCd, @Param("now") LocalDateTime now);

    /**
     * 초대 등록
     */
    int insertInvitation(JntBrupInvtn invitation);

    /**
     * 초대 대상 반려동물 등록
     */
    int insertTargetPet(InvtnTrgtPet targetPet);

    /**
     * 초대 상태 이력 등록
     */
    int insertStatusHistory(JntBrupInvtnSttsHstry history);

    /**
     * 대기 초대 응답 처리
     */
    int updateResponse(@Param("jntBrupInvtnSn") Long jntBrupInvtnSn, @Param("rcvrMbrSn") Long rcvrMbrSn,
            @Param("pendingCd") String pendingCd, @Param("aftrSttsCd") String aftrSttsCd,
            @Param("actorId") String actorId);

    /**
     * 반려동물별 수락 여부 저장
     */
    int updateTargetPetsAcceptance(@Param("jntBrupInvtnSn") Long jntBrupInvtnSn,
            @Param("acceptedPetSns") List<Long> acceptedPetSns);
}
