package com.nyangmeong.health.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * [프로필 체중 동기화 가짜]
 * - pet 프로필 체중 동기화 service 메서드(SJM) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 저장하지 않고 로그만 남김
 */
@Slf4j
@Component
public class PetWeightFake {

    /**
     * 프로필 체중을 최신 체중 기록으로 맞춤
     */
    public void sync(Long petSn, BigDecimal weight) {
        log.debug("프로필 체중 동기화(가짜): petSn={}, weight={}", petSn, weight);
    }
}
