package com.nyangmeong.global.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [역지오코딩 설정]
 * - app.region.client : fake(전주시), fake-outside(전북 밖), fake-none(찾지 못함)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.region")
public class RegionProperties {

    /** 역지오코딩 클라이언트 종류 */
    private String client = "fake";
}
