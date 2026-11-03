package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.CheckupAttachment;
import com.nyangmeong.health.dto.CheckupAttachmentSearchCond;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;

/**
 * [검진기록 첨부 매퍼]
 * - CKUP_RCD_ATCH
 */
@Mapper
public interface CheckupAttachmentMapper {

    /**
     * 검진기록 첨부 목록 조회
     */
    List<CheckupAttachmentSummary> selectList(@Param("cond") CheckupAttachmentSearchCond cond);

    /**
     * 검진기록들의 첨부 목록 조회
     */
    List<CheckupAttachmentSummary> selectListByCheckups(@Param("ckupRcdSns") List<Long> ckupRcdSns,
            @Param("cond") CheckupAttachmentSearchCond cond);

    /**
     * 검진기록 첨부 단건 조회 (목록 행 형태)
     */
    CheckupAttachmentSummary selectSummary(Long ckupRcdAtchSn);

    /**
     * 검진기록 첨부 조회
     */
    CheckupAttachment selectAttachment(Long ckupRcdAtchSn);

    /**
     * 검진기록 첨부 등록
     */
    int insertAttachment(CheckupAttachment attachment);

    /**
     * 검진기록 첨부 수정
     */
    int updateAttachment(@Param("attachment") CheckupAttachment attachment, @Param("actorId") String actorId);

    /**
     * 검진기록 첨부 삭제
     */
    int deleteAttachment(@Param("ckupRcdAtchSn") Long ckupRcdAtchSn, @Param("actorId") String actorId);

    /**
     * 반려동물의 검진기록 첨부 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
