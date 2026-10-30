package com.nyangmeong.global.code;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [지역 Mapper]
 */
@Mapper
public interface RegionMapper {

    /**
     * 하위 지역 목록 조회 (upRgnSn 없으면 시도)
     */
    List<RegionSummary> selectList(@Param("upRgnSn") Long upRgnSn);

    /**
     * 행정구역코드로 지역 번호 조회
     */
    Long selectRgnSnByAdmdstCd(@Param("admdstCd") String admdstCd);
}
