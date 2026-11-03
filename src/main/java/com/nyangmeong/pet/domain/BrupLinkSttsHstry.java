package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [양육 연동 상태 이력 VO]
 * - BRUP_LINK_STTS_HSTRY 1:1
 */
@Data
public class BrupLinkSttsHstry {
    private Long brupLinkSttsHstrySn;
    private Long brupRelSn;
    private Long mbrSn;
    private String linkChgTypeCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
