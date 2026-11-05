package com.nyangmeong.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.dto.AiRouteResponse;
import com.nyangmeong.ai.mapper.AiRouteMapper;

import lombok.RequiredArgsConstructor;

/**
 * [AI 서버용 라우팅 매핑표 서비스]
 * - FN-ADM-080-04 : AI 서버는 DB에 붙지 않아 이 조회로 매핑표를 받는다
 */
@Service
@RequiredArgsConstructor
public class AiRouteService {

    private final AiRouteMapper aiRouteMapper;

    /**
     * 사용 중 매핑 (우선순위 순)
     */
    @Transactional(readOnly = true)
    public List<AiRouteResponse> getActiveRoutes() {
        return aiRouteMapper.selectActiveRoutes().stream().map(AiRouteResponse::from).toList();
    }
}
