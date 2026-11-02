package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [픽셀 캐릭터 VO]
 * - PIXEL_CHAR 1:1
 */
@Data
public class PixelCharacter {
    private Long pixelCharSn;
    private Long mbrSn;
    private Long petSn;
    private String bscCharSbstYn;
    private Long bodySheetFileSn;
    private Double sheetScale;
    private Double sheetOriginX;
    private Double sheetOriginY;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String mdfrId;
}
