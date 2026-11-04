package com.nyangmeong.pet.dto;

import com.nyangmeong.pet.domain.CareMsn;

/**
 * [관리자 케어미션 목록 항목]
 * - 사용 안 함 미션 포함 (FN-ADM-071-01)
 */
public record AdminCareMissionSummary(Long careMsnSn, String msnPoolCd, String msnNm, Integer cmpnPnt,
        Integer achvCrtrNmtm, String mvmnPath, boolean useYn) {

    public static AdminCareMissionSummary from(CareMsn careMsn) {
        return new AdminCareMissionSummary(careMsn.getCareMsnSn(), careMsn.getMsnPoolCd(), careMsn.getMsnNm(),
                careMsn.getCmpnPnt(), careMsn.getAchvCrtrNmtm(), careMsn.getMvmnPath(),
                "Y".equals(careMsn.getUseYn()));
    }
}
