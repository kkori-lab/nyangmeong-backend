package com.nyangmeong.global.setting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [시스템 설정 Mapper]
 */
@Mapper
public interface SystemSettingMapper {

    /**
     * 현재 시스템 설정 조회
     */
    SystemSetting selectCurrent();

    /**
     * 시스템 설정 저장 (행이 없으면 SN=1로 추가)
     */
    int upsert(@Param("setting") SystemSetting setting, @Param("actorId") String actorId);
}
