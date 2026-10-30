package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.Breed;

/**
 * [품종 Mapper]
 */
@Mapper
public interface BreedMapper {

    /**
     * 구분별 사용 중인 품종 목록 조회
     */
    List<Breed> selectList(@Param("petSeCd") String petSeCd);

    /**
     * 사용 중인 품종 단건 조회
     */
    Breed selectBreed(@Param("vrtySn") Long vrtySn);
}
