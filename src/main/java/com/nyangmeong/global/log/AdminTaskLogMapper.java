package com.nyangmeong.global.log;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [관리자 작업 로그 Mapper]
 * - 수정 쿼리는 두지 않음. 삭제는 보관 일수가 지난 로그를 지우는 배치용만 (FN-ADM-114-04)
 */
@Mapper
public interface AdminTaskLogMapper {

    /**
     * 관리자 작업 로그 추가
     */
    int insert(AdminTaskLog log);

    /**
     * 관리자 작업 로그 목록 조회
     */
    List<AdminTaskLogSummary> selectList(AdminTaskLogSearchCond cond);

    /**
     * 관리자 작업 로그 단건 조회
     */
    AdminTaskLog selectById(Long mngrTaskLogSn);

    /**
     * 로그에 나온 관리자 목록 조회
     */
    List<AdminTaskLogAdminSummary> selectAdmins();

    /**
     * 관리자 닉네임 조회
     */
    String selectNcnm(Long mbrSn);

    /**
     * 기준 시각보다 앞선 관리자 작업 로그 삭제
     */
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
