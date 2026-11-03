package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [관찰 회차 목록 검색 조건]
 * - 항목 하나의 회차를 관찰일 오름차순으로 (FN-HLT-002-17~19)
 */
@Data
public class ObservationCycleSearchCond {

    /** 반려동물일련번호 (경로에서 채움) */
    private Long petSn;
    /** 관찰항목일련번호 */
    @NotNull
    private Long obsrvArtclSn;
    /** 관찰회차일련번호 (응답용 단건 조회에서만 서비스가 채움) */
    private Long obsrvCyclSn;
}
