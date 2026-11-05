package com.nyangmeong.global.aitool;

import java.util.List;

/**
 * [AI 매니저 도구 목록 응답]
 * - version : 코드 값 포함 해시, structureVersion : 코드 값 제외 해시
 */
public record AiToolCatalogResponse(String version, String structureVersion, List<AiToolResponse> tools) {
}
