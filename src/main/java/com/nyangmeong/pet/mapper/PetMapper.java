package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.BrupLinkSttsHstry;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetSummary;

/**
 * [반려동물 Mapper]
 * - PET·BRUP_REL 조회·저장·삭제 (반려동물 관리), BRUP_LINK_STTS_HSTRY 저장 (연동 해제·초대 수락)
 */
@Mapper
public interface PetMapper {

    /**
     * 회원의 양육 반려동물 목록 조회
     */
    List<PetSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("coParentRoleCd") String coParentRoleCd);

    /**
     * 반려동물 단건 조회
     */
    Pet selectPet(@Param("petSn") Long petSn);

    /**
     * 품종명 조회
     */
    String selectVrtyNm(@Param("vrtySn") Long vrtySn);

    /**
     * 구분에 맞는 사용 중 품종 여부
     */
    boolean existsVrty(@Param("vrtySn") Long vrtySn, @Param("petSeCd") String petSeCd);

    /**
     * 회원의 양육관계 조회
     */
    BrupRel selectBrupRel(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn);

    /**
     * 반려동물의 양육자 목록 조회
     */
    List<CaregiverSummary> selectCaregivers(@Param("petSn") Long petSn, @Param("primaryRoleCd") String primaryRoleCd,
            @Param("linkTypeCd") String linkTypeCd);

    /**
     * 사용 중인 양육관계 단건 조회
     */
    BrupRel selectBrupRelBySn(@Param("brupRelSn") Long brupRelSn);

    /**
     * 반려동물의 연동 양육자 회원 번호 조회
     */
    List<Long> selectCaregiverMbrSns(@Param("petSn") Long petSn);

    /**
     * 해제된 행까지 포함한 회원의 양육관계 조회
     */
    BrupRel selectBrupRelIncludingUnlinked(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn);

    /**
     * 반려동물의 연동 중 공동양육 관계 조회
     */
    List<BrupRel> selectLinkedCoParentRels(@Param("petSn") Long petSn, @Param("coParentRoleCd") String coParentRoleCd);

    /**
     * 반려동물의 연동 중 공동양육자 수
     */
    int countLinkedCaregivers(@Param("petSn") Long petSn, @Param("coParentRoleCd") String coParentRoleCd);

    /**
     * 회원의 주양육 반려동물 수
     */
    int countPrimaryPets(@Param("mbrSn") Long mbrSn, @Param("primaryRoleCd") String primaryRoleCd);

    /**
     * 반려동물 등록
     */
    int insertPet(Pet pet);

    /**
     * 양육관계 등록
     */
    int insertBrupRel(BrupRel rel);

    /**
     * 반려동물 수정
     */
    int updatePet(@Param("pet") Pet pet, @Param("actorId") String actorId);

    /**
     * 반려동물 삭제
     */
    int updatePetDelete(@Param("petSn") Long petSn, @Param("actorId") String actorId);

    /**
     * 회원의 대표 해제
     */
    int updateRepresentativeOff(@Param("mbrSn") Long mbrSn, @Param("actorId") String actorId);

    /**
     * 대표 지정
     */
    int updateRepresentativeOn(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);

    /**
     * 양육관계 연동 해제
     */
    int updateBrupRelUnlink(@Param("brupRelSn") Long brupRelSn, @Param("actorId") String actorId);

    /**
     * 해제된 양육관계 재연동
     */
    int updateBrupRelRelink(@Param("brupRelSn") Long brupRelSn, @Param("coParentRoleCd") String coParentRoleCd,
            @Param("actorId") String actorId);

    /**
     * 양육 연동 상태 이력 등록
     */
    int insertLinkHistory(BrupLinkSttsHstry history);
}
