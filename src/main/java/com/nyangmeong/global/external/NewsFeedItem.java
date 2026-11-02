package com.nyangmeong.global.external;

import java.time.LocalDateTime;

/**
 * [뉴스 피드 항목]
 * - RSS 한 건 : 제목은 끝의 언론사명을 뗀 값, link 는 피드가 준 주소 그대로
 */
public record NewsFeedItem(String title, String link, String pressNm, LocalDateTime pblcnDt) {
}
