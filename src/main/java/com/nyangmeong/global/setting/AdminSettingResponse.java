package com.nyangmeong.global.setting;

import java.time.LocalDateTime;

/**
 * [운영 설정 응답]
 * - GET·PATCH /api/admin/settings : 묶음별 객체
 */
public record AdminSettingResponse(MemberPet memberPet, Community community, Notification notification, Ai ai,
        File file, LocalDateTime mdfcnDt, String mdfrId) {

    public record MemberPet(Long petMaxCnt, Long jntBrupMaxNope, Long invtnVldDayCnt, Long lgnFailLmtNmtm,
            Long whdwlMbrKpngDayCnt, Long dshbdCareMaxExpsrCnt) {
    }

    public record Community(Long pstImgMaxCnt, Long blindDclrAcmlCnt, Long lostTpofExpryCrtrDayCnt,
            Long mtchNoRspnsEndCrtrDayCnt, String lowstHrwgAplcnYr, Long lowstHrwgAmt, String dscvAutoCmntCn,
            String aiCmtryBscCn) {
    }

    public record Notification(Long notiHideCrtrDayCnt) {
    }

    public record Ai(Long aiCnvrsTurnCnt, Long aiTknUplmtCnt) {
    }

    public record File(Long fileMaxSz) {
    }

    public static AdminSettingResponse from(SystemSetting s) {
        return new AdminSettingResponse(
                new MemberPet(s.getPetMaxCnt(), s.getJntBrupMaxNope(), s.getInvtnVldDayCnt(),
                        s.getLgnFailLmtNmtm(), s.getWhdwlMbrKpngDayCnt(), s.getDshbdCareMaxExpsrCnt()),
                new Community(s.getPstImgMaxCnt(), s.getBlindDclrAcmlCnt(), s.getLostTpofExpryCrtrDayCnt(),
                        s.getMtchNoRspnsEndCrtrDayCnt(), s.getLowstHrwgAplcnYr(), s.getLowstHrwgAmt(),
                        s.getDscvAutoCmntCn(), s.getAiCmtryBscCn()),
                new Notification(s.getNotiHideCrtrDayCnt()),
                new Ai(s.getAiCnvrsTurnCnt(), s.getAiTknUplmtCnt()),
                new File(s.getFileMaxSz()),
                s.getMdfcnDt(), s.getMdfrId());
    }
}
