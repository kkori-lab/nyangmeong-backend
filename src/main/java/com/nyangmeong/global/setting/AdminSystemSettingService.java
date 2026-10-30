package com.nyangmeong.global.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.security.domain.LoginMember;

import lombok.RequiredArgsConstructor;

/**
 * [운영 설정 수정]
 * - SYS_STNG 1행, 보낸 필드만 덮어쓰고 바뀐 항목의 전후 값만 감사 로그에 남김
 * - 조회가 매번 DB를 읽음 -> 저장하면 바로 반영 (읽기 쪽은 SystemSettingService)
 */
@Service
@RequiredArgsConstructor
public class AdminSystemSettingService {

    private static final String TARGET_TABLE = "SYS_STNG";
    private static final long TARGET_SN = 1L;

    private final SystemSettingService systemSettingService;
    private final SystemSettingMapper systemSettingMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 운영 설정 조회
     */
    @Transactional(readOnly = true)
    public SystemSetting get() {
        return systemSettingService.get();
    }

    /**
     * 운영 설정 수정
     */
    @Transactional
    public SystemSetting update(LoginMember me, AdminSettingUpdateRequest req) {
        SystemSetting current = systemSettingService.get();
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        change(before, after, "petMaxCnt", current.getPetMaxCnt(), req.petMaxCnt(), current::setPetMaxCnt);
        change(before, after, "jntBrupMaxNope", current.getJntBrupMaxNope(), req.jntBrupMaxNope(),
                current::setJntBrupMaxNope);
        change(before, after, "invtnVldDayCnt", current.getInvtnVldDayCnt(), req.invtnVldDayCnt(),
                current::setInvtnVldDayCnt);
        change(before, after, "lgnFailLmtNmtm", current.getLgnFailLmtNmtm(), req.lgnFailLmtNmtm(),
                current::setLgnFailLmtNmtm);
        change(before, after, "whdwlMbrKpngDayCnt", current.getWhdwlMbrKpngDayCnt(), req.whdwlMbrKpngDayCnt(),
                current::setWhdwlMbrKpngDayCnt);
        change(before, after, "dshbdCareMaxExpsrCnt", current.getDshbdCareMaxExpsrCnt(),
                req.dshbdCareMaxExpsrCnt(), current::setDshbdCareMaxExpsrCnt);
        change(before, after, "pstImgMaxCnt", current.getPstImgMaxCnt(), req.pstImgMaxCnt(),
                current::setPstImgMaxCnt);
        change(before, after, "blindDclrAcmlCnt", current.getBlindDclrAcmlCnt(), req.blindDclrAcmlCnt(),
                current::setBlindDclrAcmlCnt);
        change(before, after, "lostTpofExpryCrtrDayCnt", current.getLostTpofExpryCrtrDayCnt(),
                req.lostTpofExpryCrtrDayCnt(), current::setLostTpofExpryCrtrDayCnt);
        change(before, after, "mtchNoRspnsEndCrtrDayCnt", current.getMtchNoRspnsEndCrtrDayCnt(),
                req.mtchNoRspnsEndCrtrDayCnt(), current::setMtchNoRspnsEndCrtrDayCnt);
        change(before, after, "lowstHrwgAplcnYr", current.getLowstHrwgAplcnYr(), req.lowstHrwgAplcnYr(),
                current::setLowstHrwgAplcnYr);
        change(before, after, "lowstHrwgAmt", current.getLowstHrwgAmt(), req.lowstHrwgAmt(),
                current::setLowstHrwgAmt);
        change(before, after, "dscvAutoCmntCn", current.getDscvAutoCmntCn(), req.dscvAutoCmntCn(),
                current::setDscvAutoCmntCn);
        change(before, after, "aiCmtryBscCn", current.getAiCmtryBscCn(), req.aiCmtryBscCn(),
                current::setAiCmtryBscCn);
        change(before, after, "notiHideCrtrDayCnt", current.getNotiHideCrtrDayCnt(), req.notiHideCrtrDayCnt(),
                current::setNotiHideCrtrDayCnt);
        change(before, after, "aiCnvrsTurnCnt", current.getAiCnvrsTurnCnt(), req.aiCnvrsTurnCnt(),
                current::setAiCnvrsTurnCnt);
        change(before, after, "aiTknUplmtCnt", current.getAiTknUplmtCnt(), req.aiTknUplmtCnt(),
                current::setAiTknUplmtCnt);
        change(before, after, "fileMaxSz", current.getFileMaxSz(), req.fileMaxSz(), current::setFileMaxSz);

        if (after.isEmpty()) {
            return current;
        }

        systemSettingMapper.upsert(current, String.valueOf(me.id()));
        adminTaskLogService.record(me.id(), AdminTaskType.UPDATE, TARGET_TABLE, TARGET_SN,
                "운영 설정 수정: " + String.join(", ", after.keySet()), before, after);
        return systemSettingService.get();
    }

    /** 현재 값과 다른 요청값만 반영, 전후 값 수집 */
    private static <T> void change(Map<String, Object> before, Map<String, Object> after, String name, T current,
            T requested, Consumer<T> setter) {
        if (requested == null || Objects.equals(requested, current)) {
            return;
        }
        before.put(name, current);
        after.put(name, requested);
        setter.accept(requested);
    }
}
