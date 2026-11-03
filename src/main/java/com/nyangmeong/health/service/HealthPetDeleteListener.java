package com.nyangmeong.health.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.health.mapper.CareItemMapper;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupAttachmentMapper;
import com.nyangmeong.health.mapper.CheckupMapper;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.health.mapper.EtcInspectionMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;
import com.nyangmeong.health.mapper.ObservationMapper;
import com.nyangmeong.health.mapper.PrescriptionMapper;
import com.nyangmeong.health.mapper.RepeatPreventionMapper;
import com.nyangmeong.pet.domain.PetDeletedEvent;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 삭제 시 건강기록 삭제]
 * - PetDeletedEvent 를 반려동물 삭제와 같은 트랜잭션에서 받아 health 기록을 DEL_YN = 'Y' (FN-MYP-002-07)
 * - 예외를 잡지 않음 : 여기서 실패하면 반려동물 삭제도 함께 롤백
 * - 자식부터 삭제, 검진기록 쪽은 연결 일정 삭제 여부와 무관하게 SCHDL.PET_SN 으로 찾음
 */
@Component
@RequiredArgsConstructor
public class HealthPetDeleteListener {

    private static final String SYSTEM_ID = "SYSTEM";

    private final CareItemMapper careItemMapper;
    private final RepeatPreventionMapper repeatPreventionMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final InspectionValueMapper inspectionValueMapper;
    private final EtcInspectionMapper etcInspectionMapper;
    private final CheckupAttachmentMapper checkupAttachmentMapper;
    private final CheckupMapper checkupMapper;
    private final CareScheduleMapper careScheduleMapper;
    private final ObservationMapper observationMapper;
    private final DailyHealthRecordMapper dailyHealthRecordMapper;

    /**
     * 반려동물의 건강기록 전체 삭제
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPetDeleted(PetDeletedEvent event) {
        Long petSn = event.petSn();
        careItemMapper.deleteCompletionsByPetSn(petSn, SYSTEM_ID);
        careItemMapper.deleteByPetSn(petSn, SYSTEM_ID);
        repeatPreventionMapper.deleteByPetSn(petSn, SYSTEM_ID);
        prescriptionMapper.deleteByPetSn(petSn, SYSTEM_ID);
        inspectionValueMapper.deleteByPetSn(petSn, SYSTEM_ID);
        etcInspectionMapper.deleteByPetSn(petSn, SYSTEM_ID);
        checkupAttachmentMapper.deleteByPetSn(petSn, SYSTEM_ID);
        checkupMapper.deleteByPetSn(petSn, SYSTEM_ID);
        careScheduleMapper.deleteByPetSn(petSn, SYSTEM_ID);
        observationMapper.deleteCyclesByPetSn(petSn, SYSTEM_ID);
        observationMapper.deleteByPetSn(petSn, SYSTEM_ID);
        dailyHealthRecordMapper.deleteByPetSn(petSn, SYSTEM_ID);
    }
}
