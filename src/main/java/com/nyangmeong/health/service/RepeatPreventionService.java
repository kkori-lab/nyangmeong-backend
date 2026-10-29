package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.RepeatPrevention;
import com.nyangmeong.health.dto.RepeatPreventionCreateRequest;
import com.nyangmeong.health.dto.RepeatPreventionResponse;
import com.nyangmeong.health.dto.RepeatPreventionSearchCond;
import com.nyangmeong.health.dto.RepeatPreventionSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.RepeatPreventionMapper;

import lombok.RequiredArgsConstructor;

/**
 * [반복 예방 관리 서비스]
 * - 반복 예방 목록·등록·해지 (SCR-HLT-005, FN-HLT-005-05)
 * - 등록 시 정기관리 케어 항목 자동 추가, 해지 시 삭제 : 완료 기록 유지
 */
@Service
@RequiredArgsConstructor
public class RepeatPreventionService {

    private static final int MAX_DAY_CNT = 9999;

    private final RepeatPreventionMapper repeatPreventionMapper;
    private final CareScheduleMapper careScheduleMapper;
    private final CheckupService checkupService;
    private final CareItemService careItemService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 반복 예방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RepeatPreventionSummary> search(Long mbrSn, Long petSn, RepeatPreventionSearchCond cond) {
        checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        return repeatPreventionMapper.selectList(cond);
    }

    /**
     * 반복 예방 등록
     */
    @Transactional
    public RepeatPreventionResponse create(Long mbrSn, Long petSn, RepeatPreventionCreateRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        if (!careScheduleMapper.existsChildCode(HealthCodeGroup.REPEAT_PREVENTION_ARTICLE.getCode(),
                req.reptPrvntArtclCd())) {
            throw invalid();
        }
        CareCycle cycle = CareCycle.of(req.reptCycleCd()).orElseThrow(this::invalid);
        Integer dayCnt = req.cycleDrctInptDayCnt();
        if (cycle == CareCycle.CUSTOM && (dayCnt == null || dayCnt < 1 || dayCnt > MAX_DAY_CNT)) {
            throw invalid();
        }
        String actorId = String.valueOf(mbrSn);

        RepeatPrevention prevention = new RepeatPrevention();
        prevention.setPetSn(petSn);
        prevention.setCkupRcdSn(req.ckupRcdSn());
        prevention.setReptPrvntArtclCd(req.reptPrvntArtclCd());
        prevention.setReptCycleCd(cycle.getCode());
        prevention.setCycleDrctInptDayCnt(cycle == CareCycle.CUSTOM ? dayCnt : null);
        prevention.setBgngYmd(req.bgngYmd());
        prevention.setRgtrId(actorId);
        prevention.setMdfrId(actorId);
        repeatPreventionMapper.insertPrevention(prevention);

        String careArtclNm = repeatPreventionMapper.selectCodeName(prevention.getReptPrvntArtclCd());
        LocalDate nxtmPrnmntYmd = careItemService.createPreventionItem(mbrSn, prevention, careArtclNm);
        return RepeatPreventionResponse.from(prevention, nxtmPrnmntYmd);
    }

    /**
     * 반복 예방 해지
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long reptPrvntMngSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        RepeatPrevention prevention = repeatPreventionMapper.selectPrevention(reptPrvntMngSn);
        if (prevention == null || !checkupService.existsCheckup(petSn, prevention.getCkupRcdSn())) {
            throw new CustomException(ErrorCode.REPEAT_PREVENTION_NOT_FOUND);
        }
        String actorId = String.valueOf(mbrSn);
        repeatPreventionMapper.deletePrevention(reptPrvntMngSn, actorId);
        careItemService.deletePreventionItem(reptPrvntMngSn, actorId);
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
