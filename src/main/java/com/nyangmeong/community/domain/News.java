package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [뉴스 VO]
 * - NEWS 1:1
 */
@Data
public class News {
    private Long newsSn;
    private Long atchFileSn;
    private String newsClsfCd;
    private String newsSrcCd;
    private String newsTtl;
    private String newsCn;
    private String srcPressNm;
    private String orgtxtPath;
    private LocalDateTime pblcnDt;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String thmbUrlPath;
}
