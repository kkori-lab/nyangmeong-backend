package com.nyangmeong.ai.dto;

import java.util.List;

/**
 * [AI 등록 카드 컨텍스트 - 반려동물 1마리]
 * - petNm : 문장 속 이름으로 반려동물을 정한다 (FN-AIM-012-04)
 * - careArtclNms : 같은 이름 케어 항목 안내 (FN-AIM-012-08)
 */
public record RegisterContextPet(Long petSn, String petNm, List<String> careArtclNms) {
}
