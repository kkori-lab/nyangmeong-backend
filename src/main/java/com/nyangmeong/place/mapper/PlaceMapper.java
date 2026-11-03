package com.nyangmeong.place.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.place.domain.Place;
import com.nyangmeong.place.dto.PlaceSummary;

/**
 * [장소 Mapper]
 * - PLC
 */
@Mapper
public interface PlaceMapper {

    /**
     * 카카오 검색 결과 저장 : 카카오장소ID가 있으면 이름·주소·전화·좌표·URL만 갱신
     */
    int upsertKakaoPlaces(@Param("places") List<Place> places);

    /**
     * 카카오장소ID 목록으로 사용 중인 장소 조회, 내 즐겨찾기 번호 포함
     */
    List<PlaceSummary> selectByKakaoPlcIds(@Param("mbrSn") Long mbrSn, @Param("kakaoPlcIds") List<String> kakaoPlcIds);

    /**
     * 사용 중인 장소 존재 여부
     */
    boolean existsUsablePlace(@Param("plcSn") Long plcSn);
}
