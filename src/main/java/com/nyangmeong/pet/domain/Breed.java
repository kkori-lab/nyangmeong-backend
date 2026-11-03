package com.nyangmeong.pet.domain;

import lombok.Data;

/**
 * [품종 VO]
 * - VRTY 1:1 (조회용 컬럼만)
 */
@Data
public class Breed {
    private Long vrtySn;
    private String vrtyCd;
    private String petSeCd;
    private String vrtyNm;
    private Integer sortSeq;
}
