package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 캐릭터 생성 작업 목록 항목]
 * - 결과 이미지·원본 사진은 주지 않는다 (결정 26)
 */
@Data
public class AdminCharacterJobRow {
    private Long charCrtJobSn;
    private Long mbrSn;
    private String mbrNcnm;
    private String crtJobSttsCd;
    private String failRsn;
    private Long otoInqrySn;
    private LocalDateTime regDt;
}
