package com.nyangmeong.health.dto;

import java.time.LocalDate;

import com.nyangmeong.health.domain.RepeatPrevention;

/**
 * [반복 예방 응답]
 * - nxtmPrnmntYmd : 자동 추가한 케어 항목의 다음 예정일
 */
public record RepeatPreventionResponse(
        Long reptPrvntMngSn,
        Long ckupRcdSn,
        String reptPrvntArtclCd,
        String reptCycleCd,
        Integer cycleDrctInptDayCnt,
        LocalDate bgngYmd,
        LocalDate nxtmPrnmntYmd) {

    public static RepeatPreventionResponse from(RepeatPrevention prvnt, LocalDate nxtmPrnmntYmd) {
        return new RepeatPreventionResponse(prvnt.getReptPrvntMngSn(), prvnt.getCkupRcdSn(),
                prvnt.getReptPrvntArtclCd(), prvnt.getReptCycleCd(), prvnt.getCycleDrctInptDayCnt(),
                prvnt.getBgngYmd(), nxtmPrnmntYmd);
    }
}
