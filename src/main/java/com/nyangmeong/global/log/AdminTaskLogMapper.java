package com.nyangmeong.global.log;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * [관리자 작업 로그 Mapper]
 * - 추가만 있고 수정·삭제 쿼리는 두지 않음 (FN-ADM-114-04)
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
}
