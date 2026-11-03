package com.nyangmeong.ai.dto;

/**
 * [AI 설정값 응답]
 * - aiTknUplmtCnt : 응답 최대 생성 토큰 수 (FN-AIM-002-02)
 * - aiCnvrsTurnCnt : 컨텍스트로 넘기는 최근 대화 턴 수 (FN-AIM-002-11)
 */
public record AiSettingResponse(int aiTknUplmtCnt, int aiCnvrsTurnCnt) {
}
