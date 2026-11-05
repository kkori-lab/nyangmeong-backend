package com.nyangmeong.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.dto.DashboardWidgetSummary;

/**
 * [회원 메인 위젯 순서 Mapper]
 */
@Mapper
public interface DashboardWidgetMapper {

    /**
     * 사용 중인 위젯 코드 전부를 회원이 저장한 순서로, 저장한 적 없는 코드는 공통코드 기본 순서로 뒤에
     */
    List<DashboardWidgetSummary> selectWidgetList(@Param("mbrSn") Long mbrSn, @Param("prntComCd") String prntComCd);

    /**
     * 위젯 하나의 순서 저장 (이미 있으면 순서만 바꾼다)
     */
    int upsertWidgetSort(@Param("mbrSn") Long mbrSn, @Param("dshbdWdgtCd") String dshbdWdgtCd,
            @Param("sortSeq") int sortSeq);
}
