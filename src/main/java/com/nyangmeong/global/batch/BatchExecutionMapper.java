package com.nyangmeong.global.batch;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [배치 실행 이력 Mapper]
 */
@Mapper
public interface BatchExecutionMapper {

    /**
     * 실행 시작 추가
     */
    int insert(BatchExecution execution);

    /**
     * 실행 결과 수정
     */
    int updateResult(BatchExecution execution);

    /**
     * 끝나지 못한 실행을 실패로 수정
     */
    int updateInterrupted(@Param("runningCd") String runningCd, @Param("failCd") String failCd,
            @Param("failRsn") String failRsn);

    /**
     * 배치별 마지막 실행 조회
     */
    List<BatchExecution> selectLatestList();

    /**
     * 배치 실행 이력 목록 조회
     */
    List<BatchExecutionSummary> selectList(String btchSeCd);
}
