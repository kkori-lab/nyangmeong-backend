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

    /**
     * 관리자 품종 목록 조회 (사용 안 함 포함, petSeCd 가 null 이면 전체)
     */
    List<Breed> selectAdminList(@Param("petSeCd") String petSeCd);

    /**
     * 관리자 품종 단건 조회 (사용 안 함 포함)
     */
    Breed selectAdminBreed(@Param("vrtySn") Long vrtySn);

    /**
     * 같은 구분에 공백을 지운 이름이 같은 품종 수 (excludeVrtySn 은 빼고 셈)
     */
    int countSameName(@Param("petSeCd") String petSeCd, @Param("nameKey") String nameKey,
            @Param("excludeVrtySn") Long excludeVrtySn);

    /**
     * 코드 접두의 가장 큰 번호 (없으면 0)
     */
    int selectMaxCodeNo(@Param("prefix") String prefix);

    /**
     * 구분의 가장 큰 정렬 순서 (없으면 0)
     */
    int selectMaxSortSeq(@Param("petSeCd") String petSeCd);

    /**
     * 품종 등록
     */
    int insert(Breed breed);

    /**
     * 품종 수정 (null 이 아닌 칸만)
     */
    int update(Breed breed);
}
