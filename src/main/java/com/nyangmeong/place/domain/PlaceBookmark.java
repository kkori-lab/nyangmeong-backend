package com.nyangmeong.place.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [장소 즐겨찾기 VO]
 * - PLC_BKMK 1:1
 */
@Data
public class PlaceBookmark {
    /** 장소즐겨찾기일련번호 */
    private Long plcBkmkSn;
    /** 장소일련번호 */
    private Long plcSn;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 사용여부 */
    private String useYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
}
