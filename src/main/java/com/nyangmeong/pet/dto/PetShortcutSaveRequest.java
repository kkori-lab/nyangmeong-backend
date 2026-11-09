package com.nyangmeong.pet.dto;

import java.util.List;

/**
 * [반려동물 메인 바로가기 저장 요청 한 줄]
 * - PUT /api/pets/main-shortcuts 본문은 이 줄의 배열 (FN-MAN-008)
 * - 본문이 배열이라 @Valid 대신 서비스가 검사 : 칸 이름은 "[i].petSn" 형식
 */
public record PetShortcutSaveRequest(Long petSn, List<String> shrtcCds) {
}
