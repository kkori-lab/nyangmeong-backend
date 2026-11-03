package com.nyangmeong.place.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.place.domain.PlaceBookmark;
import com.nyangmeong.place.dto.PlaceBookmarkSummary;

/**
 * [장소 즐겨찾기 Mapper]
 * - PLC_BKMK
 */
@Mapper
public interface PlaceBookmarkMapper {

    /**
     * 내 즐겨찾기 조회 : 해제한 행 포함
     */
    PlaceBookmark selectByMemberAndPlace(@Param("mbrSn") Long mbrSn, @Param("plcSn") Long plcSn);

    /**
     * 즐겨찾기 등록
     */
    int insert(PlaceBookmark placeBookmark);

    /**
     * 사용 여부 변경 : 지금 값이 fromUseYn 일 때만
     */
    int updateUseYn(@Param("plcBkmkSn") Long plcBkmkSn, @Param("mbrSn") Long mbrSn,
            @Param("fromUseYn") String fromUseYn, @Param("toUseYn") String toUseYn);

    /**
     * 내 즐겨찾기 목록 조회 : 카테고리별, 저장 최신순
     */
    List<PlaceBookmarkSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("plcClsfCd") String plcClsfCd);
}
