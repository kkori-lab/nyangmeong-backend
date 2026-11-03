package com.nyangmeong.health.dto;

import java.time.LocalDate;

import com.nyangmeong.health.domain.CareCompletion;
import com.nyangmeong.health.domain.CareItem;

/**
 * [케어 완료 체크 응답]
 */
public record CareCompletionResponse(
        Long careCmptnHstrySn,
        Long careArtclSn,
        LocalDate cmptnYmd,
        Integer cmptnCycl,
        LocalDate nxtmPrnmntYmd,
        LocalDate lastCmptnYmd,
        Integer todayCmptnCnt) {

    public static CareCompletionResponse from(CareCompletion completion, CareItem item, Integer todayCmptnCnt) {
        return new CareCompletionResponse(completion.getCareCmptnHstrySn(), completion.getCareArtclSn(),
                completion.getCmptnYmd(), completion.getCmptnCycl(), item.getNxtmPrnmntYmd(),
                item.getLastCmptnYmd(), todayCmptnCnt);
    }
}
