package com.nyangmeong.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.ai.domain.KwrdDict;

/**
 * [키워드 사전 Mapper]
 * - KWRD_DICT 중 일정 등록 판정·가계부 분류 용도, 삭제는 DEL_YN = 'Y'
 */
@Mapper
public interface AiKeywordMapper {

    /**
     * 관리자 목록 (사용 안 함 포함, 정렬순서·번호순). kwrdClsfNm이 null이면 그 용도 전체
     */
    List<KwrdDict> selectAdminKeywords(@Param("dictUsgCd") String dictUsgCd,
            @Param("kwrdClsfNm") String kwrdClsfNm);

    /**
     * 키워드 한 행, 없거나 삭제됐으면 null
     */
    KwrdDict selectKeyword(Long kwrdDictSn);

    /**
     * 사용 중 행 (정렬순서·번호순) : AI 서버용
     */
    List<KwrdDict> selectActiveKeywords(@Param("dictUsgCd") String dictUsgCd);

    /**
     * 같은 용도·분류에서 삭제 안 된 행의 가장 큰 정렬순서, 없으면 null
     */
    Integer selectMaxSortSeq(@Param("dictUsgCd") String dictUsgCd, @Param("kwrdClsfNm") String kwrdClsfNm);

    /**
     * codeGroup 그룹에 속한 코드면 1
     */
    int countCodeInGroup(@Param("comCd") String comCd, @Param("codeGroup") String codeGroup);

    int insertKeyword(@Param("keyword") KwrdDict keyword, @Param("actorId") String actorId);

    /**
     * 항목명·키워드·해석 결과·연결 코드·비고·사용 여부를 keyword 값으로 덮어쓴다
     */
    int updateKeyword(@Param("keyword") KwrdDict keyword, @Param("actorId") String actorId);

    int deleteKeyword(@Param("kwrdDictSn") Long kwrdDictSn, @Param("actorId") String actorId);
}
