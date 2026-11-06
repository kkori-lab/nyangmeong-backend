package com.nyangmeong.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.ai.domain.AiRoutMpng;

/**
 * [AI 라우팅 매핑 Mapper]
 * - AI_ROUT_MPNG, 삭제는 DEL_YN = 'Y'
 */
@Mapper
public interface AiRouteMapper {

    /**
     * 관리자 매핑표 (사용 안 함 포함, 우선순위 순). q는 공백을 뺀 지시어에 들어 있는 행, null이면 전체
     */
    List<AiRoutMpng> selectAdminRoutes(@Param("q") String q);

    /**
     * 매핑 한 행, 없거나 삭제됐으면 null
     */
    AiRoutMpng selectRoute(Long aiRoutMpngSn);

    /**
     * 삭제 안 된 행 전부의 번호·우선순위 (우선순위 순, 행 잠금) : 순서를 다시 매길 때
     */
    List<AiRoutMpng> selectRouteOrderForUpdate();

    /**
     * 사용 중 행 (우선순위 순) : AI 서버용
     */
    List<AiRoutMpng> selectActiveRoutes();

    int insertRoute(@Param("route") AiRoutMpng route, @Param("actorId") String actorId);

    /**
     * 우선순위 밖 칸 전부를 route 값으로 덮어쓴다
     */
    int updateRoute(@Param("route") AiRoutMpng route, @Param("actorId") String actorId);

    int updatePrrtSeq(@Param("aiRoutMpngSn") Long aiRoutMpngSn, @Param("prrtSeq") int prrtSeq,
            @Param("actorId") String actorId);

    int deleteRoute(@Param("aiRoutMpngSn") Long aiRoutMpngSn, @Param("actorId") String actorId);
}
