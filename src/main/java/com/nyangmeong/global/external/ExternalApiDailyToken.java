package com.nyangmeong.global.external;

import java.time.LocalDate;

import lombok.Data;

/**
 * [AI 일별 토큰 사용량 응답]
 */
@Data
public class ExternalApiDailyToken {
    /** 호출일자 */
    private LocalDate callYmd;
    /** 입력 토큰 합계 */
    private long inptTknCnt;
    /** 출력 토큰 합계 */
    private long otptTknCnt;
}
