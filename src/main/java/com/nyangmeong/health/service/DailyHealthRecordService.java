package com.nyangmeong.health.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.DailyHealthRecord;
import com.nyangmeong.health.domain.DailyRecordIndicator;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.dto.DailyHealthRecordCreateRequest;
import com.nyangmeong.health.dto.DailyHealthRecordResponse;
import com.nyangmeong.health.dto.DailyHealthRecordSearchCond;
import com.nyangmeong.health.dto.DailyHealthRecordSummary;
import com.nyangmeong.health.dto.DailyHealthRecordUpdateRequest;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

import lombok.RequiredArgsConstructor;

/**
 * [일상 건강기록 서비스]
 * - 일상 건강기록 목록·등록·수정·삭제 (SCR-HLT-002, FN-HLT-002-09~13)
 * - 날짜별 합계는 묶지 않고 프론트가 묶음 (docs/api/health.md)
 */
@Service
@RequiredArgsConstructor
public class DailyHealthRecordService {

    private static final Map<DailyRecordIndicator, CareMission> MISSIONS = new EnumMap<>(DailyRecordIndicator.class);

    static {
        MISSIONS.put(DailyRecordIndicator.WEIGHT, CareMission.WEIGHT_RECORD);
        MISSIONS.put(DailyRecordIndicator.MEAL, CareMission.MEAL_RECORD);
        MISSIONS.put(DailyRecordIndicator.WATER, CareMission.WATER_RECORD);
        MISSIONS.put(DailyRecordIndicator.STOOL, CareMission.STOOL_RECORD);
        MISSIONS.put(DailyRecordIndicator.WALK, CareMission.WALK_RECORD);
    }

    private final DailyHealthRecordMapper dailyHealthRecordMapper;
    private final PetAuthorizer petAuthorizer;
    private final CareMissionService careMissionService;
    private final PetWeightFake petWeightFake;

    /**
     * 일상 건강기록 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DailyHealthRecordSummary> search(Long mbrSn, Long petSn, DailyHealthRecordSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        if (cond.getDailyRcdIdctCd() != null && DailyRecordIndicator.of(cond.getDailyRcdIdctCd()).isEmpty()) {
            throw invalid();
        }
        if (cond.getFromYmd() != null && cond.getToYmd() != null && cond.getFromYmd().isAfter(cond.getToYmd())) {
            throw invalid();
        }
        cond.setPetSn(petSn);
        return dailyHealthRecordMapper.selectList(cond);
    }

    /**
     * 일상 건강기록 등록
     */
    @Transactional
    public DailyHealthRecordResponse create(Long mbrSn, Long petSn, DailyHealthRecordCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        DailyRecordIndicator indicator = DailyRecordIndicator.of(req.dailyRcdIdctCd()).orElseThrow(this::invalid);
        checkValue(indicator, req.rcdNvl(), req.crsdSttsCd());
        String actorId = String.valueOf(mbrSn);

        DailyHealthRecord record = new DailyHealthRecord();
        record.setPetSn(petSn);
        record.setDailyRcdIdctCd(indicator.getCode());
        record.setRcdNvl(req.rcdNvl());
        record.setCrsdSttsCd(req.crsdSttsCd());
        record.setRcdDt(req.rcdDt());
        record.setRgtrId(actorId);
        record.setMdfrId(actorId);
        dailyHealthRecordMapper.insertRecord(record);

        careMissionService.reflect(petSn, MISSIONS.get(indicator), mbrSn);
        syncWeight(indicator, petSn);
        return DailyHealthRecordResponse.from(dailyHealthRecordMapper.selectSummary(record.getDailyHlthRcdSn()));
    }

    /**
     * 일상 건강기록 수정
     * - 보낸 값만 수정, 지표와 맞지 않는 필드는 400
     */
    @Transactional
    public DailyHealthRecordResponse update(Long mbrSn, Long petSn, Long dailyHlthRcdSn,
            DailyHealthRecordUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        DailyHealthRecord record = getOwn(petSn, dailyHlthRcdSn);
        DailyRecordIndicator indicator = DailyRecordIndicator.of(record.getDailyRcdIdctCd())
                .orElseThrow(this::invalid);

        if (indicator.isStool()) {
            if (req.rcdNvl() != null) {
                throw invalid();
            }
            if (req.crsdSttsCd() != null) {
                checkStoolStatus(req.crsdSttsCd());
                record.setCrsdSttsCd(req.crsdSttsCd());
            }
        } else {
            if (req.crsdSttsCd() != null) {
                throw invalid();
            }
            if (req.rcdNvl() != null) {
                checkPositive(req.rcdNvl());
                record.setRcdNvl(req.rcdNvl());
            }
        }
        if (req.rcdDt() != null) {
            record.setRcdDt(req.rcdDt());
        }
        dailyHealthRecordMapper.updateRecord(record, String.valueOf(mbrSn));
        syncWeight(indicator, petSn);
        return DailyHealthRecordResponse.from(dailyHealthRecordMapper.selectSummary(dailyHlthRcdSn));
    }

    /**
     * 일상 건강기록 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long dailyHlthRcdSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        DailyHealthRecord record = getOwn(petSn, dailyHlthRcdSn);
        dailyHealthRecordMapper.deleteRecord(dailyHlthRcdSn, String.valueOf(mbrSn));
        DailyRecordIndicator.of(record.getDailyRcdIdctCd()).ifPresent(indicator -> syncWeight(indicator, petSn));
    }

    /**
     * 경로 반려동물의 일상 건강기록 조회
     */
    private DailyHealthRecord getOwn(Long petSn, Long dailyHlthRcdSn) {
        DailyHealthRecord record = dailyHealthRecordMapper.selectRecord(petSn, dailyHlthRcdSn);
        if (record == null) {
            throw new CustomException(ErrorCode.DAILY_HEALTH_RECORD_NOT_FOUND);
        }
        return record;
    }

    /**
     * 등록 값 검사
     * - 배변상태 : 상태 코드만, 그 밖 : 수치만
     */
    private void checkValue(DailyRecordIndicator indicator, BigDecimal rcdNvl, String crsdSttsCd) {
        if (indicator.isStool()) {
            if (rcdNvl != null || crsdSttsCd == null) {
                throw invalid();
            }
            checkStoolStatus(crsdSttsCd);
            return;
        }
        if (crsdSttsCd != null || rcdNvl == null) {
            throw invalid();
        }
        checkPositive(rcdNvl);
    }

    private void checkStoolStatus(String crsdSttsCd) {
        if (!dailyHealthRecordMapper.existsChildCode(HealthCodeGroup.STOOL_STATUS.getCode(), crsdSttsCd)) {
            throw invalid();
        }
    }

    private void checkPositive(BigDecimal rcdNvl) {
        if (rcdNvl.signum() <= 0) {
            throw invalid();
        }
    }

    /**
     * 체중 기록이 바뀌면 남은 최신 체중으로 프로필 동기화(남은 기록이 없으면 하지 않음)
     */
    private void syncWeight(DailyRecordIndicator indicator, Long petSn) {
        if (indicator != DailyRecordIndicator.WEIGHT) {
            return;
        }
        BigDecimal latest = dailyHealthRecordMapper.selectLatestWeight(petSn, DailyRecordIndicator.WEIGHT.getCode());
        if (latest != null) {
            petWeightFake.sync(petSn, latest);
        }
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
