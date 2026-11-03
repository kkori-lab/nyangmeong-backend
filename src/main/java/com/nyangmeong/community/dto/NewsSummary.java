package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [뉴스 목록 항목]
 * - GET /api/news (FN-CMU-003-01)
 * - newsCn : 목록에는 앞 200자만
 */
@Data
public class NewsSummary {
    private Long newsSn;
    private String newsClsfCd;
    private String newsSrcCd;
    private String newsSrcCdNm;
    private String srcPressNm;
    private String newsTtl;
    private String newsCn;
    private LocalDateTime pblcnDt;
    private String thmbUrlPath;
    private Long atchFileSn;
}
