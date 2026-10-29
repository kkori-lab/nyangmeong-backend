package com.nyangmeong.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * [케어 항목 이름 가짜]
 * - health 케어 항목 목록 조회(JAJ, SCR-HLT-003 토탈 케어리스트) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 빈 맵 : 모든 반려동물의 케어 항목 이름이 빈 목록
 */
@Component
public class CareArticleNameFake {

    /**
     * 반려동물별 케어 항목 이름 조회 (삭제 제외, 등록순)
     */
    public Map<Long, List<String>> searchNames(List<Long> petSns) {
        return Map.of();
    }
}
