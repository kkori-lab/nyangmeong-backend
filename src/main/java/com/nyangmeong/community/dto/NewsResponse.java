package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [뉴스 상세 응답]
 * - GET /api/news/{newsSn} (FN-CMU-003-04)
 * - 외부 기사의 newsCn 은 요약만, 전문은 orgtxtPath 로 안내
 */
@Data
public class NewsResponse {
    private Long newsSn;
    private String newsClsfCd;
    private String newsSrcCd;
    private String newsSrcCdNm;
    private String srcPressNm;
    private String newsTtl;
    private String newsCn;
    private String orgtxtPath;
    private LocalDateTime pblcnDt;
    private String thmbUrlPath;
    private Long atchFileSn;
}
