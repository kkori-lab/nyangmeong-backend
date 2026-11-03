package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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
import com.nyangmeong.health.dto.NearbyScheduleSummary;
import com.nyangmeong.health.dto.ReceiptCheckupCommand;
import com.nyangmeong.health.dto.ReceiptCheckupSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupMapper;

import lombok.RequiredArgsConstructor;

/**
 * [검진기록 서비스]
 * - SCR-HLT-002 검진기록 목록·직접 등록·수정·삭제
 * - 검진기록은 같은 반려동물·같은 날짜 병원 일정에 연결하고 없으면 일정을 만든다
 * - 영수증 저장(expense)은 영수증 번호만 값으로 넘긴다. 영수증 소유·상태는 호출하는 쪽에서 검사
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
     * 반려동물의 완료 상태 검진기록 전체 조회 (검진일 최신순)
     * - 종합 건강기록의 첨부·검사 수치·처방약·반복 예방 전체 조회용 (FN-HLT-011)
     */
    @Transactional(readOnly = true)
    public List<CheckupSummary> searchAllCompleted(Long mbrSn, Long petSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return checkupMapper.selectList(petSn, new CheckupSearchCond(), ScheduleKind.HOSPITAL.getCode(),
                CheckupStatus.COMPLETED.getCode());
    }

    /**
     * 검진기록 상세 조회
     */
    @Transactional(readOnly = true)
    public CheckupResponse getCheckup(Long mbrSn, Long petSn, Long ckupRcdSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return getOwn(petSn, ckupRcdSn);
    }

    /**
     * 반려동물의 완료 상태 검진기록 여부
     * - 첨부·처방약 등 하위 기록 소유 확인용 : 반려동물 권한은 호출하는 쪽에서 검사
     */
    @Transactional(readOnly = true)
    public boolean existsCheckup(Long petSn, Long ckupRcdSn) {
        if (ckupRcdSn == null) {
            return false;
        }
        CheckupResponse response = checkupMapper.selectDetail(ckupRcdSn, CheckupStatus.COMPLETED.getCode());
        return response != null && petSn.equals(response.getPetSn());
    }

    /**
     * 검진기록 직접 등록
     */
    @Transactional
    public CheckupResponse create(Long mbrSn, Long petSn, CheckupCreateRequest req) {
        return create(mbrSn, petSn, req, null);
    }

    /**
     * 검진기록 직접 등록 (영수증 연결)
     * - 영수증 인식 실패·일시 오류 뒤 [직접 입력]으로 연 경우 recptSn 을 연결한다. 없으면 연결 없음 (FN-HLT-002-31)
     */
    @Transactional
    public CheckupResponse create(Long mbrSn, Long petSn, CheckupCreateRequest req, Long recptSn) {
        Long ckupRcdSn = insert(mbrSn, petSn, new ReceiptCheckupCommand(recptSn, req.ckupYmd(), req.hsptlNm(),
                req.ckupTypeCd(), req.ckupCn(), req.ckupMemoCn(), req.schdlSn()), CheckupSource.DIRECT);
        return getOwn(petSn, ckupRcdSn);
    }

    /**
     * 영수증 확인 후 저장의 검진기록 등록 (출처 영수증)
     * - 처방약은 호출하는 쪽이 PrescriptionService 로 따로 등록한다 (FN-HLT-002-45)
     */
    @Transactional
    public CheckupResponse createFromReceipt(Long mbrSn, Long petSn, ReceiptCheckupCommand command) {
        Long ckupRcdSn = insert(mbrSn, petSn, command, CheckupSource.RECEIPT_OCR);
        return getOwn(petSn, ckupRcdSn);
    }

    /**
     * 영수증으로 만든 완료 상태 검진기록 목록 (같은 요청 재전송 응답용)
     * - 영수증 소유는 호출하는 쪽에서 검사
     */
    @Transactional(readOnly = true)
    public List<ReceiptCheckupSummary> findReceiptCheckups(Long recptSn) {
        return checkupMapper.selectReceiptCheckups(recptSn, CheckupStatus.COMPLETED.getCode());
    }

    /**
     * 같은 날짜 병원 일정 존재 여부
     * - 반려동물 권한은 호출하는 쪽에서 검사
     */
    @Transactional(readOnly = true)
    public boolean existsHospitalSchedule(Long petSn, LocalDate ymd) {
        return checkupMapper.selectHospitalScheduleSn(petSn, ymd, ScheduleKind.HOSPITAL.getCode()) != null;
    }

    /**
     * 검진일 앞뒤 7일 안의 검진기록 미연결 병원 일정 중 가장 가까운 1건 (FN-HLT-002-46)
     * - 반려동물 권한은 호출하는 쪽에서 검사
     */
    @Transactional(readOnly = true)
    public Optional<NearbyScheduleSummary> findNearestUnlinkedSchedule(Long petSn, LocalDate ymd) {
        return Optional.ofNullable(checkupMapper.selectNearestUnlinkedHospitalSchedule(petSn, ymd, NEARBY_DAYS,
                ScheduleKind.HOSPITAL.getCode(), CheckupStatus.COMPLETED.getCode()));
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
     * 검진기록 등록 공통 : 일정 연결·생성, 완료 상태 이력
     */
    private Long insert(Long mbrSn, Long petSn, ReceiptCheckupCommand command, CheckupSource source) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        String hsptlNm = requiredText(command.hsptlNm());
        checkType(command.ckupTypeCd());
        String actorId = String.valueOf(mbrSn);

        Long schdlSn;
        if (command.schdlSn() != null) {
            schdlSn = moveAndLinkSchedule(petSn, command.schdlSn(), command.ckupYmd(), actorId);
        } else {
            schdlSn = findOrCreateSchedule(petSn, command.ckupYmd(), command.ckupTypeCd(), hsptlNm, actorId);
        }

        CheckupRecord record = new CheckupRecord();
        record.setRecptSn(command.recptSn());
        record.setSchdlSn(schdlSn);
        record.setCkupTypeCd(command.ckupTypeCd());
        record.setCkupRcdSttsCd(CheckupStatus.COMPLETED.getCode());
        record.setRcdSrcCd(source.getCode());
        record.setHsptlNm(hsptlNm);
        record.setCkupCn(emptyToNull(command.ckupCn()));
        record.setCkupMemoCn(emptyToNull(command.ckupMemoCn()));
        record.setRgtrId(actorId);
        record.setMdfrId(actorId);
        checkupMapper.insertCheckup(record);
        checkupMapper.insertStatusHistory(record.getCkupRcdSn(), mbrSn, null, CheckupStatus.COMPLETED.getCode());
        return record.getCkupRcdSn();
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
