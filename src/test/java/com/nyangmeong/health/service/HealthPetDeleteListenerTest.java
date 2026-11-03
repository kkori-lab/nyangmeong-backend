package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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

/**
 * [검사 - 반려동물 삭제 시 건강기록 삭제]
 * - 모든 건강기록 Mapper 삭제를 반려동물 번호로 호출, 실패하면 예외를 밖으로 (FN-MYP-002-07)
 */
class HealthPetDeleteListenerTest {

    private static final Long PET = 10L;
    private static final String SYSTEM = "SYSTEM";

    private final CareItemMapper careItemMapper = mock(CareItemMapper.class);
    private final RepeatPreventionMapper repeatPreventionMapper = mock(RepeatPreventionMapper.class);
    private final PrescriptionMapper prescriptionMapper = mock(PrescriptionMapper.class);
    private final InspectionValueMapper inspectionValueMapper = mock(InspectionValueMapper.class);
    private final EtcInspectionMapper etcInspectionMapper = mock(EtcInspectionMapper.class);
    private final CheckupAttachmentMapper checkupAttachmentMapper = mock(CheckupAttachmentMapper.class);
    private final CheckupMapper checkupMapper = mock(CheckupMapper.class);
    private final CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
    private final ObservationMapper observationMapper = mock(ObservationMapper.class);
    private final DailyHealthRecordMapper dailyHealthRecordMapper = mock(DailyHealthRecordMapper.class);
    private final HealthPetDeleteListener listener = new HealthPetDeleteListener(careItemMapper,
            repeatPreventionMapper, prescriptionMapper, inspectionValueMapper, etcInspectionMapper,
            checkupAttachmentMapper, checkupMapper, careScheduleMapper, observationMapper, dailyHealthRecordMapper);

    @Test
    void 반려동물의_건강기록_삭제를_모두_반려동물_번호로_호출한다() {
        listener.onPetDeleted(new PetDeletedEvent(PET));

        verify(careItemMapper).deleteCompletionsByPetSn(PET, SYSTEM);
        verify(careItemMapper).deleteByPetSn(PET, SYSTEM);
        verify(repeatPreventionMapper).deleteByPetSn(PET, SYSTEM);
        verify(prescriptionMapper).deleteByPetSn(PET, SYSTEM);
        verify(inspectionValueMapper).deleteByPetSn(PET, SYSTEM);
        verify(etcInspectionMapper).deleteByPetSn(PET, SYSTEM);
        verify(checkupAttachmentMapper).deleteByPetSn(PET, SYSTEM);
        verify(checkupMapper).deleteByPetSn(PET, SYSTEM);
        verify(careScheduleMapper).deleteByPetSn(PET, SYSTEM);
        verify(observationMapper).deleteCyclesByPetSn(PET, SYSTEM);
        verify(observationMapper).deleteByPetSn(PET, SYSTEM);
        verify(dailyHealthRecordMapper).deleteByPetSn(PET, SYSTEM);
    }

    @Test
    void 자식_기록을_부모보다_먼저_지운다() {
        listener.onPetDeleted(new PetDeletedEvent(PET));

        InOrder order = inOrder(careItemMapper, prescriptionMapper, checkupMapper, careScheduleMapper,
                observationMapper);
        order.verify(careItemMapper).deleteCompletionsByPetSn(PET, SYSTEM);
        order.verify(careItemMapper).deleteByPetSn(PET, SYSTEM);
        order.verify(prescriptionMapper).deleteByPetSn(PET, SYSTEM);
        order.verify(checkupMapper).deleteByPetSn(PET, SYSTEM);
        order.verify(careScheduleMapper).deleteByPetSn(PET, SYSTEM);
        order.verify(observationMapper).deleteCyclesByPetSn(PET, SYSTEM);
        order.verify(observationMapper).deleteByPetSn(PET, SYSTEM);
    }

    @Test
    void 하나가_실패하면_예외를_삼키지_않고_밖으로_던진다() {
        IllegalStateException failure = new IllegalStateException("DB 오류");
        when(checkupMapper.deleteByPetSn(anyLong(), any())).thenThrow(failure);

        assertThatThrownBy(() -> listener.onPetDeleted(new PetDeletedEvent(PET))).isSameAs(failure);
        verify(careScheduleMapper, never()).deleteByPetSn(anyLong(), any());
        verify(dailyHealthRecordMapper, never()).deleteByPetSn(anyLong(), any());
    }

    @Test
    void 반려동물_삭제와_같은_트랜잭션의_커밋_직전에_받는다() throws Exception {
        TransactionalEventListener annotation = HealthPetDeleteListener.class
                .getMethod("onPetDeleted", PetDeletedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }
}
