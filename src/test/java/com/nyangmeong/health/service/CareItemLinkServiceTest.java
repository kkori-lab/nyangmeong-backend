package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.CareZone;
import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.domain.RepeatPrevention;
import com.nyangmeong.health.mapper.CareItemMapper;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 처방약·반복 예방 연동 케어 항목]
 * - 투약 항목은 일상관리, 반복 예방 항목은 정기관리 + 첫 예정일 = 시작일 (FN-HLT-005-03·05)
 */
class CareItemLinkServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long PRSC = 70L;
    private static final Long PRVNT = 80L;
    private static final LocalDate BGNG = LocalDate.of(2026, 9, 23);

    private final CareItemMapper mapper = mock(CareItemMapper.class);
    private final CareItemService service = new CareItemService(mapper, mock(PetAuthorizer.class),
            mock(CareMissionService.class));

    private Prescription prescription() {
        Prescription prescription = new Prescription();
        prescription.setPrscSn(PRSC);
        prescription.setMdcnNm("소염제");
        return prescription;
    }

    @Test
    void 투약_항목은_처방약에_연결한_일상관리_항목이다() {
        service.createMedicationItem(MBR, PET, prescription());

        verify(mapper).insertItem(argThat(item -> PRSC.equals(item.getPrscSn()) && PET.equals(item.getPetSn())
                && CareZone.DAILY.is(item.getCareZoneCd()) && "소염제".equals(item.getCareArtclNm())
                && "N".equals(item.getBscArtclYn()) && item.getCareCycleCd() == null));
    }

    @Test
    void 이미_투약_항목이_있으면_다시_만들지_않는다() {
        when(mapper.existsPrescriptionItem(PRSC)).thenReturn(true);

        service.createMedicationItem(MBR, PET, prescription());

        verify(mapper, never()).insertItem(any());
    }

    @Test
    void 반복_예방_항목은_시작일을_첫_예정일로_하는_정기관리_항목이다() {
        RepeatPrevention prevention = new RepeatPrevention();
        prevention.setReptPrvntMngSn(PRVNT);
        prevention.setPetSn(PET);
        prevention.setReptCycleCd(CareCycle.MONTH_1.getCode());
        prevention.setBgngYmd(BGNG);

        LocalDate next = service.createPreventionItem(MBR, prevention, "심장사상충 예방");

        assertThat(next).isEqualTo(BGNG);
        verify(mapper).insertItem(argThat(item -> PRVNT.equals(item.getReptPrvntMngSn())
                && CareZone.REGULAR.is(item.getCareZoneCd()) && CareCycle.MONTH_1.is(item.getCareCycleCd())
                && BGNG.equals(item.getNxtmPrnmntYmd()) && "심장사상충 예방".equals(item.getCareArtclNm())));
    }

    @Test
    void 연동_항목_삭제는_처방약과_반복_예방별로_지운다() {
        service.deleteMedicationItem(PRSC, "1");
        service.deletePreventionItem(PRVNT, "1");

        verify(mapper).deletePrescriptionItems(PRSC, "1");
        verify(mapper).deleteRepeatPreventionItems(PRVNT, "1");
    }
}
