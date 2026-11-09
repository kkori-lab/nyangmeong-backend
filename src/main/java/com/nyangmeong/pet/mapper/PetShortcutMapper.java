package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.PetMainShrtc;

/**
 * [반려동물 메인 바로가기 Mapper]
 */
@Mapper
public interface PetShortcutMapper {

    /**
     * 양육 중인 반려동물 번호 목록 조회
     */
    List<Long> selectCaregivingPetSns(Long mbrSn);

    /**
     * 회원의 바로가기 행 전체 조회
     */
    List<PetMainShrtc> selectList(Long mbrSn);

    /**
     * 바로가기 한 종류의 표시 여부·순서 저장
     */
    int upsert(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn, @Param("shrtcCd") String shrtcCd,
            @Param("indctYn") String indctYn, @Param("sortSeq") int sortSeq);
}
