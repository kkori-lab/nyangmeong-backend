package com.nyangmeong.global.external;

/**
 * [뉴스 원문 미리보기]
 * - 원문 페이지의 og:description, og:image : 읽지 못한 값은 null
 */
public record NewsPreview(String description, String imageUrl) {

    public static NewsPreview empty() {
        return new NewsPreview(null, null);
    }
}
