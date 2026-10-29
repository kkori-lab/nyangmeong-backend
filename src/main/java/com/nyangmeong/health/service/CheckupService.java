package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.health.domain.CheckupRecord;
import com.nyangmeong.health.domain.CheckupSource;
import com.nyangmeong.health.domain.CheckupStatus;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.Schedule;
import com.nyangmeong.health.domain.ScheduleKind;
import com.nyangmeong.health.domain.ScheduleSource;
import com.nyangmeong.health.dto.CheckupCreateRequest;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSearchCond;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.CheckupUpdateRequest;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupMapper;

import lombok.RequiredArgsConstructor;

/**
 * [검진기록 서비스]
 * - SCR-HLT-002 검진기록 목록·직접 등록·수정·삭제
 * - 검진기록은 같은 반려동물·같은 날짜 병원 일정에 연결하고 없으면 일정을 만든다
 */
@Service
@RequiredArgsConstructor
public class CheckupService {

    private static final int PAGE_SIZE = 10;
    /** [옮겨 연결하기] 대상 일정의 검진일 앞뒤 허용 일수 */
    private static final int NEARBY_DAYS = 7;

    private final CheckupMapper checkupMapper;
    private final CareScheduleMapper careScheduleMapper;
    private final PetAuthorizer petAuthorizer;

    /**
     * 검진기록 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CheckupSummary> search(Long mbrSn, Long petSn, CheckupSearchCond cond, PageQuery pq) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<CheckupSummary> rows = checkupMapper.selectList(petSn, cond, ScheduleKind.HOSPITAL.getCode(),
                CheckupStatus.COMPLETED.getCode());
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 검진기록 직접 등록
     */
    @Transactional
    public CheckupResponse create(Long mbrSn, Long petSn, CheckupCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        String hsptlNm = requiredText(req.hsptlNm());
        checkType(req.ckupTypeCd());
        String actorId = String.valueOf(mbrSn);

        Long schdlSn;
        if (req.schdlSn() != null) {
            schdlSn = moveAndLinkSchedule(petSn, req.schdlSn(), req.ckupYmd(), actorId);
        } else {
            schdlSn = findOrCreateSchedule(petSn, req.ckupYmd(), req.ckupTypeCd(), hsptlNm, actorId);
        }

        CheckupRecord record = new CheckupRecord();
        record.setSchdlSn(schdlSn);
        record.setCkupTypeCd(req.ckupTypeCd());
        record.setCkupRcdSttsCd(CheckupStatus.COMPLETED.getCode());
        record.setRcdSrcCd(CheckupSource.DIRECT.getCode());
        record.setHsptlNm(hsptlNm);
        record.setCkupCn(emptyToNull(req.ckupCn()));
        record.setCkupMemoCn(emptyToNull(req.ckupMemoCn()));
        record.setRgtrId(actorId);
        record.setMdfrId(actorId);
        checkupMapper.insertCheckup(record);
        checkupMapper.insertStatusHistory(record.getCkupRcdSn(), mbrSn, null, CheckupStatus.COMPLETED.getCode());
        return getOwn(petSn, record.getCkupRcdSn());
    }

    /**
     * 검진기록 수정
     */
    @Transactional
    public CheckupResponse update(Long mbrSn, Long petSn, Long ckupRcdSn, CheckupUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CheckupResponse current = getOwn(petSn, ckupRcdSn);
        String actorId = String.valueOf(mbrSn);

        Long targetPetSn = req.petSn() != null ? req.petSn() : petSn;
        boolean isPetChanged = !targetPetSn.equals(petSn);
        if (isPetChanged) {
            petAuthorizer.checkCaregiver(mbrSn, targetPetSn);
        }
        LocalDate targetYmd = req.ckupYmd() != null ? req.ckupYmd() : current.getCkupYmd();
        boolean isYmdChanged = req.ckupYmd() != null && !req.ckupYmd().equals(current.getCkupYmd());

        String ckupTypeCd = current.getCkupTypeCd();
        if (req.ckupTypeCd() != null) {
            checkType(req.ckupTypeCd());
            ckupTypeCd = req.ckupTypeCd();
        }
        String hsptlNm = req.hsptlNm() != null ? requiredText(req.hsptlNm()) : current.getHsptlNm();

        CheckupRecord record = new CheckupRecord();
        record.setCkupRcdSn(ckupRcdSn);
        record.setSchdlSn(current.getSchdlSn());
        record.setCkupTypeCd(ckupTypeCd);
        record.setHsptlNm(hsptlNm);
        record.setCkupCn(req.ckupCn() != null ? emptyToNull(req.ckupCn()) : current.getCkupCn());
        record.setCkupMemoCn(req.ckupMemoCn() != null ? emptyToNull(req.ckupMemoCn()) : current.getCkupMemoCn());
        /** 검진일·반려동물이 바뀌면 바뀐 쪽의 같은 날짜 병원 일정으로 다시 연결 (원래 일정은 남김) */
        if (isPetChanged || isYmdChanged) {
            record.setSchdlSn(findOrCreateSchedule(targetPetSn, targetYmd, ckupTypeCd, hsptlNm, actorId));
        }
        checkupMapper.updateCheckup(record, actorId);
        if (isPetChanged) {
            checkupMapper.updateLinkedCareItemsPet(ckupRcdSn, targetPetSn, actorId);
            checkupMapper.updateLinkedRepeatPreventionsPet(ckupRcdSn, targetPetSn, actorId);
        }
        return getOwn(targetPetSn, ckupRcdSn);
    }

    /**
     * 검진기록 삭제 (병원 일정과 완료 기록은 유지)
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long ckupRcdSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwn(petSn, ckupRcdSn);
        String actorId = String.valueOf(mbrSn);

        checkupMapper.updateStatus(ckupRcdSn, CheckupStatus.DELETED.getCode(), actorId);
        checkupMapper.insertStatusHistory(ckupRcdSn, mbrSn, CheckupStatus.COMPLETED.getCode(),
                CheckupStatus.DELETED.getCode());
        checkupMapper.deleteLinkedCareItems(ckupRcdSn, actorId);
        checkupMapper.deleteLinkedRepeatPreventions(ckupRcdSn, actorId);
    }

    /**
     * 경로 반려동물의 완료 상태 검진기록 조회
     */
    private CheckupResponse getOwn(Long petSn, Long ckupRcdSn) {
        CheckupResponse response = checkupMapper.selectDetail(ckupRcdSn, CheckupStatus.COMPLETED.getCode());
        if (response == null || !petSn.equals(response.getPetSn())) {
            throw new CustomException(ErrorCode.CHECKUP_NOT_FOUND);
        }
        return response;
    }

    /**
     * 같은 날짜 병원 일정(먼저 등록된 것)을 찾고 없으면 새로 만든다
     */
    private Long findOrCreateSchedule(Long petSn, LocalDate ymd, String ckupTypeCd, String hsptlNm, String actorId) {
        Long schdlSn = checkupMapper.selectHospitalScheduleSn(petSn, ymd, ScheduleKind.HOSPITAL.getCode());
        if (schdlSn != null) {
            return schdlSn;
        }
        Schedule schedule = new Schedule();
        schedule.setPetSn(petSn);
        schedule.setSchdlKndCd(ScheduleKind.HOSPITAL.getCode());
        schedule.setSchdlSrcCd(ScheduleSource.CHECKUP.getCode());
        schedule.setHrInptYn("N");
        schedule.setSchdlDt(ymd.atStartOfDay());
        schedule.setCkupTypeCd(ckupTypeCd);
        schedule.setHsptlNm(hsptlNm);
        schedule.setRgtrId(actorId);
        schedule.setMdfrId(actorId);
        careScheduleMapper.insertSchedule(schedule);
        return schedule.getSchdlSn();
    }

    /**
     * [옮겨 연결하기] 대상 일정 검사 후 일정 날짜를 검진일로 옮긴다 (시각·병원명·검진유형 유지)
     */
    private Long moveAndLinkSchedule(Long petSn, Long schdlSn, LocalDate ymd, String actorId) {
        Schedule schedule = careScheduleMapper.selectSchedule(schdlSn);
        if (schedule == null || !petSn.equals(schedule.getPetSn())
                || !ScheduleKind.HOSPITAL.is(schedule.getSchdlKndCd())
                || checkupMapper.existsLinkedCheckup(schdlSn, CheckupStatus.COMPLETED.getCode())
                || Math.abs(ChronoUnit.DAYS.between(schedule.getSchdlDt().toLocalDate(), ymd)) > NEARBY_DAYS) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDateTime moved = "Y".equals(schedule.getHrInptYn()) ? ymd.atTime(schedule.getSchdlDt().toLocalTime())
                : ymd.atStartOfDay();
        schedule.setSchdlDt(moved);
        careScheduleMapper.updateSchedule(schedule, actorId);
        return schdlSn;
    }

    private void checkType(String ckupTypeCd) {
        if (!careScheduleMapper.existsChildCode(HealthCodeGroup.CHECKUP_TYPE.getCode(), ckupTypeCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String requiredText(String value) {
        String text = emptyToNull(value);
        if (text == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return text;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
