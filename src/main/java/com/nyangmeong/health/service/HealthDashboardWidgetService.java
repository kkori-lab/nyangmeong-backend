package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CareZone;
import com.nyangmeong.health.domain.CheckupStatus;
import com.nyangmeong.health.domain.CheckupType;
import com.nyangmeong.health.domain.DailyRecordIndicator;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.HealthDashboardItem;
import com.nyangmeong.health.domain.ObservationStatus;
import com.nyangmeong.health.domain.ScheduleKind;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse.PetRecordedItems;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest.Widget;
import com.nyangmeong.health.dto.HealthDashboardWidgetSummary;
import com.nyangmeong.health.dto.HealthRecordedItemSummary;
import com.nyangmeong.health.mapper.HealthDashboardMapper;
import com.nyangmeong.health.mapper.HealthDashboardWidgetMapper;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

import lombok.RequiredArgsConstructor;

/**
 * [건강기록 대시보드 고른 항목 서비스]
 * - 회원이 고른 대시보드 항목·순서 조회·저장 : 회원 단위(반려동물마다 따로 두지 않음)
 * - 조회 응답에 회원의 반려동물별 기록 있는 항목을 함께 담음 : 프론트의 항목별 API 반복 호출 대체
 */
@Service
@RequiredArgsConstructor
public class HealthDashboardWidgetService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int SORT_STEP = 10;

    private final HealthDashboardWidgetMapper healthDashboardWidgetMapper;
    private final HealthDashboardMapper healthDashboardMapper;
    private final PetService petService;

    /**
     * 고른 항목과 반려동물별 기록 있는 항목 조회
     */
    @Transactional(readOnly = true)
    public HealthDashboardWidgetResponse get(Long mbrSn) {
        return read(mbrSn, LocalDate.now(ZONE));
    }

    /**
     * 고른 항목 전체 저장
     */
    @Transactional
    public HealthDashboardWidgetResponse save(Long mbrSn, HealthDashboardWidgetSaveRequest req) {
        checkAllItems(req.widgets());
        List<Widget> widgets = req.widgets();
        for (int i = 0; i < widgets.size(); i++) {
            Widget widget = widgets.get(i);
            healthDashboardWidgetMapper.upsertWidget(mbrSn, widget.dshbdWdgtCd(),
                    Boolean.TRUE.equals(widget.indctYn()) ? "Y" : "N", (i + 1) * SORT_STEP);
        }
        return read(mbrSn, LocalDate.now(ZONE));
    }

    HealthDashboardWidgetResponse read(Long mbrSn, LocalDate baseYmd) {
        List<HealthDashboardWidgetSummary> widgets = healthDashboardWidgetMapper.selectWidgetList(mbrSn,
                HealthCodeGroup.DASHBOARD_ITEM.getCode());
        boolean savedYn = widgets.stream().anyMatch(HealthDashboardWidgetSummary::isSavedYn);
        return new HealthDashboardWidgetResponse(savedYn, widgets, searchRecordedItems(mbrSn, baseYmd));
    }

    /**
     * 13개 코드를 한 번씩 담았는지 검사(모르는·중복·빠진 코드)
     */
    private void checkAllItems(List<Widget> widgets) {
        Set<String> codes = new HashSet<>();
        for (Widget widget : widgets) {
            boolean known = HealthDashboardItem.of(widget.dshbdWdgtCd()).isPresent();
            if (!known || !codes.add(widget.dshbdWdgtCd())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        if (codes.size() != HealthDashboardItem.values().length) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<PetRecordedItems> searchRecordedItems(Long mbrSn, LocalDate baseYmd) {
        List<PetSummary> pets = petService.searchMyPets(mbrSn);
        if (pets.isEmpty()) {
            return List.of();
        }
        List<Long> petSns = pets.stream().map(PetSummary::getPetSn).toList();
        Map<Long, HealthRecordedItemSummary> rows = healthDashboardMapper
                .selectRecordedItems(petSns, baseYmd, DailyRecordIndicator.WEIGHT.getCode(),
                        DailyRecordIndicator.MEAL.getCode(), DailyRecordIndicator.WATER.getCode(),
                        DailyRecordIndicator.WALK.getCode(), DailyRecordIndicator.STOOL.getCode(),
                        ScheduleKind.HOSPITAL.getCode(), CheckupStatus.COMPLETED.getCode(),
                        ObservationStatus.ONGOING.getCode(), CheckupType.VACCINATION.getCode(),
                        CheckupType.REGULAR.getCode(), CareZone.DAILY.getCode())
                .stream().collect(Collectors.toMap(HealthRecordedItemSummary::getPetSn, Function.identity()));
        List<PetRecordedItems> result = new ArrayList<>();
        for (PetSummary pet : pets) {
            HealthRecordedItemSummary row = rows.get(pet.getPetSn());
            if (row != null) {
                result.add(new PetRecordedItems(pet.getPetSn(), recordedCodes(row, hasAllergy(pet))));
            }
        }
        return result;
    }

    /**
     * 알레르기·기저질환 입력 여부
     */
    private boolean hasAllergy(PetSummary pet) {
        return hasText(pet.getAlgyCn()) || hasText(pet.getDssCn());
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /**
     * 기록 있는 항목 코드(기본 순서)
     */
    private List<String> recordedCodes(HealthRecordedItemSummary row, boolean allergy) {
        List<String> codes = new ArrayList<>();
        addIf(codes, row.isWeight(), HealthDashboardItem.WEIGHT);
        addIf(codes, row.isMeal(), HealthDashboardItem.MEAL);
        addIf(codes, row.isWater(), HealthDashboardItem.WATER);
        addIf(codes, row.isWalk(), HealthDashboardItem.WALK);
        addIf(codes, row.isPoop(), HealthDashboardItem.POOP);
        addIf(codes, row.isCkup(), HealthDashboardItem.CKUP);
        addIf(codes, row.isInsp(), HealthDashboardItem.INSP);
        addIf(codes, row.isPresc(), HealthDashboardItem.PRESC);
        addIf(codes, row.isAtch(), HealthDashboardItem.ATCH);
        addIf(codes, row.isObsv(), HealthDashboardItem.OBSV);
        addIf(codes, allergy, HealthDashboardItem.ALLERGY);
        addIf(codes, row.isVacc(), HealthDashboardItem.VACC);
        addIf(codes, row.isCare(), HealthDashboardItem.CARE);
        return codes;
    }

    private void addIf(List<String> codes, boolean recorded, HealthDashboardItem item) {
        if (recorded) {
            codes.add(item.getCode());
        }
    }
}
