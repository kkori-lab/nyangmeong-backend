package com.nyangmeong.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.nyangmeong.ai.domain.AiHealthReport;

/**
 * [AI 건강리포트 Mapper]
 */
@Mapper
public interface AiHealthReportMapper {

    /**
     * 반려동물의 저장 리포트, 없으면 null
     */
    AiHealthReport selectByPet(Long petSn);

    /**
     * 반려동물의 저장 리포트 (행 잠금), 없으면 null
     */
    AiHealthReport selectByPetForUpdate(Long petSn);

    /**
     * 리포트 저장 (반려동물 행이 있으면 덮어쓰기)
     */
    int upsert(AiHealthReport report);
}
