package com.nyangmeong.pet.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [반려동물 메인 바로가기 항목]
 * - GET·PUT /api/pets/main-shortcuts (FN-MAN-008), 바로가기 코드(MANG02)는 보일 순서대로
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetShortcutSummary {
    private Long petSn;
    private List<String> shrtcCds;
}
