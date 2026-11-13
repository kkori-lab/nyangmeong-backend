package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 뉴스 목록 항목]
 * - GET /api/admin/news (FN-ADM-050-01), 숨긴 기사 포함
 * - 썸네일 : 관리자 등록은 atchFileSn(/api/files/{atchFileSn}), 수집 기사는 thmbUrlPath(외부 URL)
 * - orgtxtPath : 수집 기사 원문 링크, 관리자 등록은 null
 */
@Data
public class AdminNewsSummary {
    private Long newsSn;
    private Long atchFileSn;
    private String newsClsfCd;
    private String newsSrcCd;
    private String newsSrcCdNm;
    private String newsTtl;
    private String newsCn;
    private String srcPressNm;
    private String orgtxtPath;
    private LocalDateTime pblcnDt;
    private boolean useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String thmbUrlPath;
}
