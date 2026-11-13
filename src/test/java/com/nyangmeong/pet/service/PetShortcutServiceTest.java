package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.PetMainShrtc;
import com.nyangmeong.pet.dto.PetShortcutSaveRequest;
import com.nyangmeong.pet.dto.PetShortcutSummary;
import com.nyangmeong.pet.mapper.PetShortcutMapper;

/**
 * [검사 - 반려동물 메인 바로가기]
 * - 저장한 적 없거나 전부 숨기면 빈 목록
 * - 형식이 틀리거나 남의 반려동물이면 아무것도 저장하지 않음
 */
class PetShortcutServiceTest {

    private static final long MBR = 7L;
    private static final String HEALTH = "MANC0008";
    private static final String DIARY = "MANC0009";
    private static final String CALENDAR = "MANC0010";

    PetShortcutMapper mapper = mock(PetShortcutMapper.class);
    CommonCodeService commonCodeService = mock(CommonCodeService.class);
    PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    PetShortcutService service;
    List<PetMainShrtc> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new PetShortcutService(mapper, commonCodeService, petAuthorizer);
        when(commonCodeService.search("MANG02")).thenReturn(codes(HEALTH, DIARY, CALENDAR));
        when(mapper.selectCaregivingPetSns(MBR)).thenReturn(List.of(1L, 2L));
        when(mapper.selectList(MBR)).thenReturn(rows);
    }

    @Test
    void 저장한_적_없는_반려동물은_빈_목록이다() {
        assertThat(service.search(MBR)).containsExactly(new PetShortcutSummary(1L, List.of()),
                new PetShortcutSummary(2L, List.of()));
    }

    @Test
    void 보이는_것만_순서대로_주고_전부_숨기면_빈_목록이다() {
        rows.add(row(1L, CALENDAR, "Y", 10));
        rows.add(row(1L, HEALTH, "Y", 20));
        rows.add(row(1L, DIARY, "N", 0));
        rows.add(row(2L, HEALTH, "N", 0));
        rows.add(row(2L, DIARY, "N", 0));
        rows.add(row(2L, CALENDAR, "N", 0));
        /** 연동 해제된 반려동물의 행 */
        rows.add(row(9L, HEALTH, "Y", 10));

        assertThat(service.search(MBR)).containsExactly(new PetShortcutSummary(1L, List.of(CALENDAR, HEALTH)),
                new PetShortcutSummary(2L, List.of()));
    }

    @Test
    void 사용하지_않는_코드는_보이지_않는다() {
        when(commonCodeService.search("MANG02")).thenReturn(codes(HEALTH, DIARY));
        rows.add(row(1L, CALENDAR, "Y", 10));
        rows.add(row(1L, DIARY, "Y", 20));

        assertThat(service.search(MBR).get(0).getShrtcCds()).containsExactly(DIARY);
    }

    @Test
    void 저장은_보이는_것에_순서를_주고_나머지는_숨긴다() {
        service.save(MBR, List.of(new PetShortcutSaveRequest(1L, List.of(CALENDAR, HEALTH))));

        verify(petAuthorizer).checkCaregiver(MBR, 1L);
        verify(mapper).upsert(MBR, 1L, CALENDAR, "Y", 10);
        verify(mapper).upsert(MBR, 1L, HEALTH, "Y", 20);
        verify(mapper).upsert(MBR, 1L, DIARY, "N", 0);
    }

    @Test
    void 빈_목록_저장은_전부_숨긴다() {
        service.save(MBR, List.of(new PetShortcutSaveRequest(1L, List.of())));

        verify(mapper).upsert(MBR, 1L, HEALTH, "N", 0);
        verify(mapper).upsert(MBR, 1L, DIARY, "N", 0);
        verify(mapper).upsert(MBR, 1L, CALENDAR, "N", 0);
    }

    @Test
    void 같은_반려동물_두_번은_400이다() {
        assertInvalid(List.of(new PetShortcutSaveRequest(1L, List.of(HEALTH)),
                new PetShortcutSaveRequest(1L, List.of(DIARY))), "[1].petSn");
    }

    @Test
    void 한_반려동물_안_같은_코드_두_번은_400이다() {
        assertInvalid(List.of(new PetShortcutSaveRequest(1L, List.of(HEALTH, HEALTH))), "[0].shrtcCds[1]");
    }

    @Test
    void 없거나_사용하지_않는_코드는_400이다() {
        assertInvalid(List.of(new PetShortcutSaveRequest(1L, List.of("MANC0001"))), "[0].shrtcCds[0]");
        when(commonCodeService.search("MANG02")).thenReturn(codes(HEALTH, DIARY));
        assertInvalid(List.of(new PetShortcutSaveRequest(1L, List.of(CALENDAR))), "[0].shrtcCds[0]");
    }

    @Test
    void 빠진_칸은_400이다() {
        assertInvalid(List.of(new PetShortcutSaveRequest(null, List.of(HEALTH))), "[0].petSn");
        assertInvalid(List.of(new PetShortcutSaveRequest(1L, null)), "[0].shrtcCds");
        assertInvalid(Arrays.asList(new PetShortcutSaveRequest[] { null }), "[0]");
    }

    @Test
    void 남의_반려동물이_하나라도_있으면_아무것도_저장하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, 2L);

        CustomException e = catchThrowableOfType(CustomException.class,
                () -> service.save(MBR, List.of(new PetShortcutSaveRequest(1L, List.of(HEALTH)),
                        new PetShortcutSaveRequest(2L, List.of(HEALTH)))));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).upsert(anyLong(), anyLong(), anyString(), anyString(), anyInt());
    }

    private void assertInvalid(List<PetShortcutSaveRequest> req, String field) {
        CustomException e = catchThrowableOfType(CustomException.class, () -> service.save(MBR, req));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(e.getFields()).containsExactly(field);
        verify(mapper, never()).upsert(anyLong(), anyLong(), anyString(), anyString(), anyInt());
    }

    private static List<CommonCodeSummary> codes(String... cds) {
        return Arrays.stream(cds).map(cd -> {
            CommonCodeSummary c = new CommonCodeSummary();
            c.setComCd(cd);
            return c;
        }).toList();
    }

    private static PetMainShrtc row(Long petSn, String cd, String indctYn, int sortSeq) {
        PetMainShrtc r = new PetMainShrtc();
        r.setMbrSn(MBR);
        r.setPetSn(petSn);
        r.setShrtcCd(cd);
        r.setIndctYn(indctYn);
        r.setSortSeq(sortSeq);
        return r;
    }
}
