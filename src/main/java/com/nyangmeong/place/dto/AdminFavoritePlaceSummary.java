package com.nyangmeong.place.dto;

import lombok.Data;

/**
 * [관리자 대시보드 즐겨찾기 상위 장소]
 * - SCR-ADM-001 (FN-ADM-001-04) : 기간 무관 지금 즐겨찾기 수
 */
@Data
public class AdminFavoritePlaceSummary {
    /** 장소일련번호 */
    private Long plcSn;
    /** 장소 상호명 */
    private String plcNm;
    /** 즐겨찾기 수 */
    private long cnt;
}
