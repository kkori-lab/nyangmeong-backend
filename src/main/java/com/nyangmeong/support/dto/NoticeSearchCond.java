package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [공지사항 검색 조건]
 * - keyword : 제목·내용 검색, 없거나 공백이면 전체 (FN-CSC-001-03)
 * - latestYn : true 면 상단 고정을 무시하고 작성일 최신순 (상세 하단 최신 공지, FN-CSC-001-06)
 */
@Data
public class NoticeSearchCond {
    private String keyword;
    private Boolean latestYn;
}
