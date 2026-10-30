package com.nyangmeong.global.log;

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
}
