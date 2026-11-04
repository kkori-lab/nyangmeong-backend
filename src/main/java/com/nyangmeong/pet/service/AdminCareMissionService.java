package com.nyangmeong.pet.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.pet.domain.CareMissionPool;
import com.nyangmeong.pet.domain.CareMsn;
import com.nyangmeong.pet.dto.AdminCareMissionSummary;
import com.nyangmeong.pet.dto.AdminCareMissionUpdateRequest;
import com.nyangmeong.pet.mapper.CareMissionMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 케어미션 서비스]
 * - SCR-ADM-071 (FN-ADM-071-01·02·03)
 * - 추가·삭제 없음 : 미션은 CareMission enum의 활동과 짝지어져 있음
 * - 바꾼 값은 다음 출제부터 반영 : 이미 출제된 미션은 DAY_MSN에 복사한 배점·달성 기준을 씀
 */
@Service
@RequiredArgsConstructor
public class AdminCareMissionService {

    private static final String TARGET_TABLE = "CARE_MSN";

    private final CareMissionMapper careMissionMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 관리자 케어미션 목록 조회 (사용 안 함 포함, 번호순)
     */
    @Transactional(readOnly = true)
    public List<AdminCareMissionSummary> search(String msnPoolCd) {
        boolean all = msnPoolCd == null || msnPoolCd.isBlank();
        if (!all && Arrays.stream(CareMissionPool.values()).noneMatch(p -> p.is(msnPoolCd))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return careMissionMapper.selectAdminMissions(all ? null : msnPoolCd).stream()
                .map(AdminCareMissionSummary::from).toList();
    }

    /**
     * 케어미션 수정 (보상 포인트·달성 기준 횟수·사용 여부 중 보낸 칸만)
     */
    @Transactional
    public AdminCareMissionSummary update(Long actorSn, Long careMsnSn, AdminCareMissionUpdateRequest req) {
        if (req.cmpnPnt() == null && req.achvCrtrNmtm() == null && req.useYn() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        CareMsn before = getOrThrow(careMsnSn);

        CareMsn careMsn = new CareMsn();
        careMsn.setCareMsnSn(careMsnSn);
        careMsn.setCmpnPnt(req.cmpnPnt());
        careMsn.setAchvCrtrNmtm(req.achvCrtrNmtm());
        careMsn.setUseYn(req.useYn() == null ? null : (req.useYn() ? "Y" : "N"));
        careMsn.setMdfrId(String.valueOf(actorSn));
        careMissionMapper.updateMission(careMsn);

        AdminCareMissionSummary updated = AdminCareMissionSummary.from(getOrThrow(careMsnSn));
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, careMsnSn, "케어미션 수정",
                AdminCareMissionSummary.from(before), updated);
        return updated;
    }

    private CareMsn getOrThrow(Long careMsnSn) {
        CareMsn careMsn = careMissionMapper.selectMission(careMsnSn);
        if (careMsn == null) {
            throw new CustomException(ErrorCode.CARE_MISSION_NOT_FOUND);
        }
        return careMsn;
    }
}
