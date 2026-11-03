package com.nyangmeong.community.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * [실종제보 VO]
 * - LOST_TPOF 1:1
 */
@Getter
@Setter
public class LostReport {
    private Long lostTpofSn;
    private Long pstSn;
    private Long rgnSn;
    private String petSeCd;
    private String lostTpofSttsCd;
    private String petNm;
    private String colrNm;
    private String cnplEnc;
    private BigDecimal wtnsPlcLat;
    private BigDecimal wtnsPlcLot;
    private Integer pstgPrdUpdtNmtm;
    private LocalDateTime lastWtnsDt;
    private LocalDateTime expryCrtrDt;
    private LocalDateTime dscvSwtcDt;
    private String wtnsPlcNm;
    private String prvcRlsAgreYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private Long vrtySn;
    private String vrtyDrctInptNm;
    private String chrcCn;
}
