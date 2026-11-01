package com.nyangmeong.global.batch;

/**
 * [배치 작업]
 * - 구현체는 빈으로 등록 -> BatchScheduleRegistrar 가 cron 등록, 관리자 수동 실행 대상
 * - @Scheduled 를 쓰지 않음 : 자기 메서드 호출은 프록시를 건너뛰어 @Transactional 이 빠짐
 */
public interface BatchJob {

    /**
     * 배치 구분
     */
    BatchType type();

    /**
     * 실행 주기 cron
     */
    String cron();

    /**
     * 배치 실행 : 처리 건수를 세지 않는 배치는 null
     */
    Integer execute();
}
