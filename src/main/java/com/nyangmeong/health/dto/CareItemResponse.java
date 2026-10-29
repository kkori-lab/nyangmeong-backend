package com.nyangmeong.health.dto;

import java.time.LocalDate;

/**
 * [케어 항목 응답]
 * - 목록 행(CareItemSummary)과 같은 필드
 */
public record CareItemResponse(
        Long careArtclSn,
        String careArtclNm,
        String careZoneCd,
        String careCycleCd,
        String careCycleCdNm,
        Integer cycleDrctInptDayCnt,
        String rprsIconId,
        LocalDate nxtmPrnmntYmd,
        LocalDate lastCmptnYmd,
        boolean mainExpsrYn,
        Integer mainSortSeq,
        boolean bscArtclYn,
        Long prscSn,
        Long reptPrvntMngSn,
        Long ckupRcdSn,
        LocalDate mdcnEndYmd,
        Integer dayMdcnNmtm,
        Integer todayCmptnCnt) {

    public static CareItemResponse from(CareItemSummary item) {
        return new CareItemResponse(item.getCareArtclSn(), item.getCareArtclNm(), item.getCareZoneCd(),
                item.getCareCycleCd(), item.getCareCycleCdNm(), item.getCycleDrctInptDayCnt(),
                item.getRprsIconId(), item.getNxtmPrnmntYmd(), item.getLastCmptnYmd(), item.isMainExpsrYn(),
                item.getMainSortSeq(), item.isBscArtclYn(), item.getPrscSn(), item.getReptPrvntMngSn(),
                item.getCkupRcdSn(), item.getMdcnEndYmd(), item.getDayMdcnNmtm(), item.getTodayCmptnCnt());
    }
}
