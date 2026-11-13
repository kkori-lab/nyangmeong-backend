package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.pet.domain.BrupLinkSttsHstry;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.domain.PetCreatedEvent;
import com.nyangmeong.pet.domain.PetDeletedEvent;
import com.nyangmeong.pet.dto.AdminPetSummary;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetCreateRequest;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.dto.PetUpdateRequest;
import com.nyangmeong.pet.mapper.CareMissionMapper;
import com.nyangmeong.pet.mapper.CaregiverInvitationMapper;
import com.nyangmeong.pet.mapper.PetMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 내 반려동물 목록·등록·수정·대표 지정·양육자 목록·연동 해제·삭제]
 */
class PetServiceTest {

    private static final Long MBR = 1L;

    private static final Long PET = 100L;
    private static final String DOG = "PETC0001";
    private static final String CAT = "PETC0002";
    private static final String MALE = "PETC0003";
    private static final String WHITE = "PETC0006";
    private static final String DIRECT_COLOR = "PETC0017";
    private static final String DOG_ONLY_COLOR = "PETC0011";
    private static final String CAT_ONLY_COLOR = "PETC0014";
    private static final String PRIMARY = "PETC0018";
    private static final String CO_PARENT = "PETC0019";
    private static final String ACCEPTED = "PETC0024";

    private final PetMapper mapper = mock(PetMapper.class);
    private final PetAuthorizer authorizer = mock(PetAuthorizer.class);
    private final CommonCodeService codes = mock(CommonCodeService.class);
    private final PixelCharacterMapper pixelMapper = mock(PixelCharacterMapper.class);
    private final EventPublisher events = mock(EventPublisher.class);
    private final CareMissionMapper missionMapper = mock(CareMissionMapper.class);
    private final CaregiverInvitationMapper invitationMapper = mock(CaregiverInvitationMapper.class);
    private final ApplicationEventPublisher domainEvents = mock(ApplicationEventPublisher.class);
    private final MemberService memberService = mock(MemberService.class);
    private final PetService service = new PetService(mapper, authorizer, codes, pixelMapper, events, missionMapper,
            invitationMapper, domainEvents, memberService);

    @BeforeEach
    void setUp() {
        when(codes.search("PETG01")).thenReturn(codeList(DOG, CAT));
        when(codes.search("PETG02")).thenReturn(codeList(MALE, "PETC0004"));
        when(codes.search("PETG03")).thenReturn(codeList(WHITE, DIRECT_COLOR, DOG_ONLY_COLOR, CAT_ONLY_COLOR));
        when(mapper.existsVrty(15L, DOG)).thenReturn(true);
        when(mapper.selectVrtyNm(15L)).thenReturn("푸들");
        doAnswer(inv -> {
            inv.<Pet>getArgument(0).setPetSn(PET);
            return 1;
        }).when(mapper).insertPet(any());
        when(mapper.selectPet(PET)).thenAnswer(inv -> storedPet());
        when(mapper.selectBrupRel(anyLong(), eq(PET))).thenReturn(rel(BrupRole.PRIMARY.getCode(), "N"));
        when(mapper.selectCaregivers(PET, PRIMARY, ACCEPTED)).thenReturn(new ArrayList<>());
    }

    @Test
    void 관리자_반려동물_목록은_없는_회원이면_404() {
        doThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND)).when(memberService).checkExists(2L);

        assertThatThrownBy(() -> service.searchForAdmin(2L, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        verify(mapper, never()).selectAdminList(any(), any());
    }

    @Test
    void 관리자_반려동물_목록은_공동양육_역할로_세고_삭제_동물도_돌려준다() {
        AdminPetSummary deleted = new AdminPetSummary();
        deleted.setDelYn(true);
        deleted.setCaregiverCount(0);
        when(mapper.selectAdminList(1L, CO_PARENT)).thenReturn(List.of(deleted));

        assertThat(service.searchForAdmin(1L, new PageQuery(null, null, null)).content()).containsExactly(deleted);
    }

    @Test
    void 목록은_공동양육자_코드로_인원을_센_결과를_그대로_돌려준다() {
        PetSummary primary = pet(10L, BrupRole.PRIMARY.getCode(), true, 2, "410000000000001");
        PetSummary coParent = pet(11L, BrupRole.CO_PARENT.getCode(), false, 1, null);
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of(primary, coParent));

        List<PetSummary> result = service.searchMyPets(MBR);

        assertThat(result).extracting(PetSummary::getPetSn).containsExactly(10L, 11L);
        assertThat(result.get(0).isRprsPetYn()).isTrue();
        assertThat(result.get(0).getCaregiverCount()).isEqualTo(2);
        assertThat(result.get(1).getBrupRoleCd()).isEqualTo(BrupRole.CO_PARENT.getCode());
    }

    @Test
    void 목록은_중성화_여부와_정상_체중_범위를_그대로_돌려준다() {
        PetSummary primary = pet(10L, BrupRole.PRIMARY.getCode(), true, 0, null);
        primary.setNutrYn(true);
        primary.setMinBdwg(new BigDecimal("3.500"));
        primary.setMaxBdwg(new BigDecimal("5.250"));
        PetSummary coParent = pet(11L, BrupRole.CO_PARENT.getCode(), false, 1, null);
        coParent.setMinBdwg(new BigDecimal("2.000"));
        coParent.setMaxBdwg(new BigDecimal("4.000"));
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of(primary, coParent));

        List<PetSummary> result = service.searchMyPets(MBR);

        assertThat(result).extracting(PetSummary::isNutrYn).containsExactly(true, false);
        assertThat(result).extracting(PetSummary::getMinBdwg)
                .containsExactly(new BigDecimal("3.500"), new BigDecimal("2.000"));
        assertThat(result).extracting(PetSummary::getMaxBdwg)
                .containsExactly(new BigDecimal("5.250"), new BigDecimal("4.000"));
    }

    @Test
    void 칩이_없는_반려동물의_chipNo는_null() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode()))
                .thenReturn(List.of(pet(10L, BrupRole.PRIMARY.getCode(), true, 0, null)));

        assertThat(service.searchMyPets(MBR).get(0).getChipNo()).isNull();
    }

    @Test
    void 양육_반려동물이_없으면_빈_목록() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of());

        assertThat(service.searchMyPets(MBR)).isEmpty();
    }

    private PetSummary pet(Long petSn, String roleCd, boolean rprs, int caregivers, String chipNo) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setBrupRoleCd(roleCd);
        pet.setRprsPetYn(rprs);
        pet.setCaregiverCount(caregivers);
        pet.setChipNo(chipNo);
        return pet;
    }

    @Test
    void 등록은_PET과_주양육_양육관계를_저장한다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(2);

        PetResponse res = service.createPet(MBR, create(15L, null, " 새우, ,닭고기 ", " , "));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).insertPet(pet.capture());
        assertThat(pet.getValue().getAlgyCn()).isEqualTo("새우,닭고기");
        assertThat(pet.getValue().getDssCn()).isNull();
        assertThat(pet.getValue().getRgtrId()).isEqualTo("1");
        ArgumentCaptor<BrupRel> rel = ArgumentCaptor.forClass(BrupRel.class);
        verify(mapper).insertBrupRel(rel.capture());
        assertThat(rel.getValue().getPetSn()).isEqualTo(PET);
        assertThat(rel.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(rel.getValue().getBrupRoleCd()).isEqualTo(BrupRole.PRIMARY.getCode());
        assertThat(rel.getValue().getRprsPetYn()).isEqualTo("N");
        assertThat(res.petSn()).isEqualTo(PET);
    }

    @Test
    void 등록하면_오늘_날짜로_등록_이벤트를_발행한다() {
        service.createPet(MBR, create(15L, null, null, null));

        verify(domainEvents).publishEvent(new PetCreatedEvent(PET, LocalDate.now(ZoneId.of("Asia/Seoul"))));
    }

    @Test
    void 출제_중_예외가_나도_반려동물_등록은_성공한다() {
        CareMissionService failing = mock(CareMissionService.class);
        doThrow(new IllegalStateException("UK_DAY_MSN_1 중복")).when(failing).issueFor(anyLong(), any(), anyBoolean());
        PetCreatedEventListener listener = new PetCreatedEventListener(failing);
        /** 커밋 뒤 리스너를 바로 부르는 이벤트 발행기 : 실제로는 AFTER_COMMIT 이라 등록 응답과 무관하다 */
        doAnswer(inv -> {
            if (inv.getArgument(0) instanceof PetCreatedEvent event) {
                listener.onPetCreated(event);
            }
            return null;
        }).when(domainEvents).publishEvent(any(Object.class));

        PetResponse res = service.createPet(MBR, create(15L, null, null, null));

        assertThat(res.petSn()).isEqualTo(PET);
        verify(failing).issueFor(eq(PET), any(), eq(false));
    }

    @Test
    void 주양육_0마리에서_등록하면_대표가_된다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(0);

        service.createPet(MBR, create(15L, null, null, null));

        ArgumentCaptor<BrupRel> rel = ArgumentCaptor.forClass(BrupRel.class);
        verify(mapper).insertBrupRel(rel.capture());
        assertThat(rel.getValue().getRprsPetYn()).isEqualTo("Y");
    }

    @Test
    void 주양육_10마리면_등록이_막힌다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(10);

        assertCode(() -> service.createPet(MBR, create(15L, null, null, null)), ErrorCode.PET_LIMIT_EXCEEDED);
        verify(mapper, never()).insertPet(any());
    }

    @Test
    void 주양육_9마리면_공동양육과_상관없이_등록된다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(9);

        service.createPet(MBR, create(15L, null, null, null));

        verify(mapper).insertPet(any());
    }

    @Test
    void 품종을_둘_다_보내거나_둘_다_없으면_400() {
        assertCode(() -> service.createPet(MBR, create(15L, "믹스", null, null)), ErrorCode.INVALID_INPUT_VALUE);
        assertCode(() -> service.createPet(MBR, create(null, null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다른_구분의_품종이면_BREED_NOT_FOUND() {
        when(mapper.existsVrty(30L, DOG)).thenReturn(false);

        assertCode(() -> service.createPet(MBR, create(30L, null, null, null)), ErrorCode.BREED_NOT_FOUND);
    }

    @Test
    void 없는_털색_코드면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("3"), kg("5"), null, "PETC9999", null, null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 강아지에_고양이_전용_털색을_보내면_400() {
        assertCode(() -> service.createPet(MBR, colorCreate(DOG, CAT_ONLY_COLOR)), ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertPet(any());
    }

    @Test
    void 고양이에_강아지_전용_털색을_보내면_400() {
        assertCode(() -> service.createPet(MBR, colorCreate(CAT, DOG_ONLY_COLOR)), ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertPet(any());
    }

    @Test
    void 고양이에_직접_입력과_공통_털색은_저장된다() {
        when(mapper.existsVrty(15L, CAT)).thenReturn(true);

        service.createPet(MBR, colorCreate(CAT, DIRECT_COLOR));
        service.createPet(MBR, colorCreate(CAT, WHITE));

        verify(mapper, times(2)).insertPet(any());
    }

    @Test
    void 수정에서_털색만_바꿔_구분과_어긋나면_400() {
        assertCode(() -> service.updatePet(MBR, PET, colorUpdate(null, CAT_ONLY_COLOR)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_구분만_바꿔_기존_털색과_어긋나면_400() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> {
            Pet pet = storedPet();
            pet.setColrCd(DOG_ONLY_COLOR);
            return pet;
        });

        assertCode(() -> service.updatePet(MBR, PET, colorUpdate(CAT, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 털색_직접_입력인데_이름이_없으면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("3"), kg("5"), null, DIRECT_COLOR, " ", null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 최소_체중이_최대_이상이면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("5"), kg("5"), null, WHITE, null, null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 공동양육자도_수정할_수_있고_보낸_필드만_바뀐다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(BrupRole.CO_PARENT.getCode(), "N"));

        service.updatePet(MBR, PET, update(null, kg("6.5"), false, null, null));

        verify(authorizer).checkCaregiver(MBR, PET);
        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getPetBdwg()).isEqualByComparingTo("6.5");
        assertThat(pet.getValue().getNutrYn()).isEqualTo("N");
        assertThat(pet.getValue().getPetNm()).isEqualTo("뭉치");
        assertThat(pet.getValue().getMaxBdwg()).isEqualByComparingTo("5");
    }

    @Test
    void 양육자가_아니면_수정이_막힌다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(authorizer).checkCaregiver(MBR, PET);

        assertCode(() -> service.updatePet(MBR, PET, update(null, kg("6.5"), null, null, null)),
                ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_최소만_보내_기존_최대_이상이면_400() {
        assertCode(() -> service.updatePet(MBR, PET, update(kg("5"), null, null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_알레르기를_정리해_저장하고_빈_목록은_비운다() {
        service.updatePet(MBR, PET, update(null, null, null, " 새우, ,닭고기 ", " , "));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getAlgyCn()).isEqualTo("새우,닭고기");
        assertThat(pet.getValue().getDssCn()).isNull();
    }

    @Test
    void 수정에서_목표체중_비우기를_보내면_null로_저장한다() {
        service.updatePet(MBR, PET, goalUpdate(null, true));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getGoalBdwg()).isNull();
    }

    @Test
    void 수정에서_목표체중과_비우기를_함께_보내면_400() {
        assertCode(() -> service.updatePet(MBR, PET, goalUpdate(kg("4.5"), true)), ErrorCode.BAD_REQUEST);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_목표체중을_보내지_않으면_그대로_둔다() {
        service.updatePet(MBR, PET, goalUpdate(null, false));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getGoalBdwg()).isEqualByComparingTo("4");
    }

    @Test
    void 상세는_공동양육자에게_주양육자와_본인_양육관계만_준다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(BrupRole.CO_PARENT.getCode(), "N"));
        when(mapper.selectCaregivers(PET, PRIMARY, ACCEPTED)).thenReturn(List.of(caregiver(9L, PRIMARY),
                caregiver(MBR, CO_PARENT), caregiver(8L, CO_PARENT)));

        PetResponse res = service.getPet(MBR, PET);

        assertThat(res.caregivers()).extracting(CaregiverSummary::getMbrSn).containsExactly(9L, MBR);
        assertThat(res.vrtyNm()).isEqualTo("푸들");
    }

    @Test
    void 대표_지정은_기존_대표를_해제한_뒤_지정한다() {
        service.designateRepresentative(MBR, PET);

        InOrder order = inOrder(authorizer, mapper);
        order.verify(authorizer).checkPrimary(MBR, PET);
        order.verify(mapper).updateRepresentativeOff(MBR, "1");
        order.verify(mapper).updateRepresentativeOn(MBR, PET, "1");
    }

    @Test
    void 공동양육_동물은_대표로_지정할_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(authorizer).checkPrimary(MBR, PET);

        assertCode(() -> service.designateRepresentative(MBR, PET), ErrorCode.PET_PRIMARY_ONLY);
        verify(mapper, never()).updateRepresentativeOff(anyLong(), anyString());
    }

    @Test
    void 양육자_목록은_주양육자에게_전원을_준다() {
        when(mapper.selectCaregivers(PET, PRIMARY, ACCEPTED)).thenReturn(List.of(caregiver(MBR, PRIMARY),
                caregiver(7L, CO_PARENT), caregiver(8L, CO_PARENT)));

        List<CaregiverSummary> result = service.searchCaregivers(MBR, PET);

        verify(authorizer).checkCaregiver(MBR, PET);
        assertThat(result).extracting(CaregiverSummary::getMbrSn).containsExactly(MBR, 7L, 8L);
    }

    @Test
    void 양육자_목록은_공동양육자에게_주양육자_첫_줄과_본인만_준다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(CO_PARENT, "N"));
        when(mapper.selectCaregivers(PET, PRIMARY, ACCEPTED)).thenReturn(List.of(caregiver(9L, PRIMARY),
                caregiver(7L, CO_PARENT), caregiver(MBR, CO_PARENT)));

        List<CaregiverSummary> result = service.searchCaregivers(MBR, PET);

        assertThat(result).extracting(CaregiverSummary::getMbrSn).containsExactly(9L, MBR);
        assertThat(result.get(0).getBrupRoleCd()).isEqualTo(PRIMARY);
    }

    @Test
    void 양육관계가_없으면_양육자_목록을_볼_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(authorizer).checkCaregiver(MBR, PET);

        assertCode(() -> service.searchCaregivers(MBR, PET), ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).selectCaregivers(anyLong(), anyString(), anyString());
    }

    @Test
    void 주양육자는_공동양육자를_강제_해제하고_이력을_남긴다() {
        when(mapper.selectBrupRelBySn(60L)).thenReturn(target(60L, 7L, CO_PARENT));
        when(mapper.updateBrupRelUnlink(60L, "1")).thenReturn(1);

        service.unlinkCaregiver(MBR, PET, 60L);

        ArgumentCaptor<BrupLinkSttsHstry> history = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        InOrder order = inOrder(authorizer, mapper);
        order.verify(authorizer).checkCaregiver(MBR, PET);
        order.verify(mapper).updateBrupRelUnlink(60L, "1");
        order.verify(mapper).insertLinkHistory(history.capture());
        assertThat(history.getValue().getBrupRelSn()).isEqualTo(60L);
        assertThat(history.getValue().getMbrSn()).isEqualTo(7L);
        assertThat(history.getValue().getLinkChgTypeCd()).isEqualTo("PETC0026");
    }

    @Test
    void 공동양육자는_본인_연동을_자진_해제한다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(CO_PARENT, "N"));
        when(mapper.selectBrupRelBySn(60L)).thenReturn(target(60L, MBR, CO_PARENT));
        when(mapper.updateBrupRelUnlink(60L, "1")).thenReturn(1);

        service.unlinkCaregiver(MBR, PET, 60L);

        ArgumentCaptor<BrupLinkSttsHstry> history = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        verify(mapper).insertLinkHistory(history.capture());
        assertThat(history.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(history.getValue().getLinkChgTypeCd()).isEqualTo("PETC0025");
    }

    @Test
    void 공동양육자는_다른_공동양육자를_해제할_수_없다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(CO_PARENT, "N"));
        when(mapper.selectBrupRelBySn(60L)).thenReturn(target(60L, 7L, CO_PARENT));

        assertCode(() -> service.unlinkCaregiver(MBR, PET, 60L), ErrorCode.PET_PRIMARY_ONLY);
        verify(mapper, never()).updateBrupRelUnlink(anyLong(), anyString());
        verify(mapper, never()).insertLinkHistory(any());
    }

    @Test
    void 주양육자_행은_해제할_수_없다() {
        when(mapper.selectBrupRelBySn(50L)).thenReturn(target(50L, MBR, PRIMARY));

        assertCode(() -> service.unlinkCaregiver(MBR, PET, 50L), ErrorCode.CAREGIVER_NOT_FOUND);
        verify(mapper, never()).updateBrupRelUnlink(anyLong(), anyString());
    }

    @Test
    void 다른_반려동물이나_해제된_연동은_404() {
        BrupRel other = target(61L, 7L, CO_PARENT);
        other.setPetSn(200L);
        when(mapper.selectBrupRelBySn(61L)).thenReturn(other);

        assertCode(() -> service.unlinkCaregiver(MBR, PET, 61L), ErrorCode.CAREGIVER_NOT_FOUND);
        assertCode(() -> service.unlinkCaregiver(MBR, PET, 62L), ErrorCode.CAREGIVER_NOT_FOUND);
        verify(mapper, never()).insertLinkHistory(any());
    }

    @Test
    void 동시에_이미_해제됐으면_이력을_남기지_않는다() {
        when(mapper.selectBrupRelBySn(60L)).thenReturn(target(60L, 7L, CO_PARENT));
        when(mapper.updateBrupRelUnlink(60L, "1")).thenReturn(0);

        assertCode(() -> service.unlinkCaregiver(MBR, PET, 60L), ErrorCode.CAREGIVER_NOT_FOUND);
        verify(mapper, never()).insertLinkHistory(any());
    }

    @Test
    void 양육관계가_없으면_연동을_해제할_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(authorizer).checkCaregiver(MBR, PET);

        assertCode(() -> service.unlinkCaregiver(MBR, PET, 60L), ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).selectBrupRelBySn(anyLong());
    }

    @Test
    void 연동_양육자_회원_번호는_Mapper_결과를_그대로_준다() {
        when(mapper.selectCaregiverMbrSns(PET)).thenReturn(List.of(MBR, 7L));

        assertThat(service.searchCaregiverMbrSns(PET)).containsExactly(MBR, 7L);
    }

    private BrupRel target(Long brupRelSn, Long mbrSn, String roleCd) {
        BrupRel rel = rel(roleCd, "N");
        rel.setBrupRelSn(brupRelSn);
        rel.setPetSn(PET);
        rel.setMbrSn(mbrSn);
        return rel;
    }

    @Test
    void 삭제는_반려동물_캐릭터_보유_아이템을_지우고_공동양육_연동을_해제하고_알린다() {
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT))
                .thenReturn(List.of(target(60L, 7L, CO_PARENT), target(61L, 8L, CO_PARENT)));
        when(mapper.updateBrupRelUnlink(anyLong(), eq("1"))).thenReturn(1);

        service.deletePet(MBR, PET);

        InOrder order = inOrder(authorizer, mapper, pixelMapper);
        order.verify(authorizer).checkPrimary(MBR, PET);
        order.verify(mapper).updatePetDelete(PET, "1");
        order.verify(pixelMapper).updateHeldItemDeleteByPetSn(PET);
        order.verify(pixelMapper).updateDeleteByPetSn(PET, "1");
        verify(mapper).updateBrupRelUnlink(60L, "1");
        verify(mapper).updateBrupRelUnlink(61L, "1");

        ArgumentCaptor<BrupLinkSttsHstry> history = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        verify(mapper, times(2)).insertLinkHistory(history.capture());
        assertThat(history.getAllValues()).extracting(BrupLinkSttsHstry::getMbrSn).containsExactly(7L, 8L);
        assertThat(history.getAllValues()).extracting(BrupLinkSttsHstry::getLinkChgTypeCd)
                .containsOnly("PETC0027");

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events, times(2)).publish(event.capture());
        assertThat(event.getAllValues()).extracting(NotificationEvent::mbrSn).containsExactly(7L, 8L);
        assertThat(event.getValue().type()).isEqualTo(NotificationType.CO_PARENTING);
        assertThat(event.getValue().notiTtl()).isEqualTo("뭉치의 기록이 정리되어 공동양육 연동이 해제되었어요");
        assertThat(event.getValue().mvmnPath()).isEqualTo("/mypage/pets");
        verify(mapper, never()).updateRepresentativeOff(anyLong(), anyString());
    }

    @Test
    void 삭제_중_연동이_동시에_해제되면_그_공동양육자의_이력과_알림을_남기지_않는다() {
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT))
                .thenReturn(List.of(target(60L, 7L, CO_PARENT), target(61L, 8L, CO_PARENT)));
        when(mapper.updateBrupRelUnlink(60L, "1")).thenReturn(0);
        when(mapper.updateBrupRelUnlink(61L, "1")).thenReturn(1);

        service.deletePet(MBR, PET);

        ArgumentCaptor<BrupLinkSttsHstry> history = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        verify(mapper, times(1)).insertLinkHistory(history.capture());
        assertThat(history.getValue().getMbrSn()).isEqualTo(8L);
        assertThat(history.getValue().getBrupRelSn()).isEqualTo(61L);
        assertThat(history.getValue().getLinkChgTypeCd()).isEqualTo("PETC0027");
        assertThat(history.getAllValues()).noneMatch(h -> Long.valueOf(7L).equals(h.getMbrSn()));

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events, times(1)).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(8L);
        assertThat(event.getAllValues()).noneMatch(e -> Long.valueOf(7L).equals(e.mbrSn()));

        verify(domainEvents).publishEvent(new PetDeletedEvent(PET));
    }

    @Test
    void 삭제는_가계부를_뺀_pet_안의_기록을_지우고_위층에_삭제_이벤트를_발행한다() {
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT)).thenReturn(List.of());

        service.deletePet(MBR, PET);

        verify(pixelMapper).updatePhotoDeleteByPetSn(PET);
        verify(pixelMapper).updateItemSheetDeleteByPetSn(PET);
        verify(pixelMapper).updateJobDeleteByPetSn(PET);
        verify(missionMapper).updateDayMissionDeleteByPetSn(PET);
        verify(invitationMapper).updateTargetPetDeleteByPetSn(PET);
        verify(domainEvents).publishEvent(new PetDeletedEvent(PET));
    }

    @Test
    void 공동양육자가_없으면_해제와_알림이_없다() {
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT)).thenReturn(List.of());

        service.deletePet(MBR, PET);

        verify(mapper, never()).updateBrupRelUnlink(anyLong(), anyString());
        verify(mapper, never()).insertLinkHistory(any());
        verify(events, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 대표를_삭제하면_남은_첫_주양육_동물이_대표가_된다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(PRIMARY, "Y"));
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT)).thenReturn(List.of());
        when(mapper.selectList(MBR, CO_PARENT)).thenReturn(List.of(pet(11L, CO_PARENT, false, 0, null),
                pet(12L, PRIMARY, false, 0, null), pet(13L, PRIMARY, false, 0, null)));

        service.deletePet(MBR, PET);

        InOrder order = inOrder(mapper);
        order.verify(mapper).updateRepresentativeOff(MBR, "1");
        order.verify(mapper).updateRepresentativeOn(MBR, 12L, "1");
    }

    @Test
    void 대표를_삭제하고_남은_주양육_동물이_없으면_대표가_없다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(PRIMARY, "Y"));
        when(mapper.updatePetDelete(PET, "1")).thenReturn(1);
        when(mapper.selectLinkedCoParentRels(PET, CO_PARENT)).thenReturn(List.of());
        when(mapper.selectList(MBR, CO_PARENT)).thenReturn(List.of(pet(11L, CO_PARENT, false, 0, null)));

        service.deletePet(MBR, PET);

        verify(mapper).updateRepresentativeOff(MBR, "1");
        verify(mapper, never()).updateRepresentativeOn(anyLong(), anyLong(), anyString());
    }

    @Test
    void 공동양육자는_삭제할_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(authorizer).checkPrimary(MBR, PET);

        assertCode(() -> service.deletePet(MBR, PET), ErrorCode.PET_PRIMARY_ONLY);
        verify(mapper, never()).updatePetDelete(anyLong(), anyString());
        verify(pixelMapper, never()).updateDeleteByPetSn(anyLong(), anyString());
        verify(events, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 동시에_이미_삭제됐으면_404이고_나머지를_하지_않는다() {
        when(mapper.updatePetDelete(PET, "1")).thenReturn(0);

        assertCode(() -> service.deletePet(MBR, PET), ErrorCode.PET_NOT_FOUND);
        verify(pixelMapper, never()).updateHeldItemDeleteByPetSn(anyLong());
        verify(mapper, never()).selectLinkedCoParentRels(anyLong(), anyString());
        verify(missionMapper, never()).updateDayMissionDeleteByPetSn(anyLong());
        verify(domainEvents, never()).publishEvent(any(Object.class));
    }

    @Test
    void 체중_동기화는_펫_체중과_수정자를_그대로_mapper에_넘긴다() {
        BigDecimal weight = new BigDecimal("5.250");
        when(mapper.updatePetWeight(PET, weight, "7")).thenReturn(1);

        service.syncWeight(PET, weight, 7L);

        verify(mapper).updatePetWeight(PET, weight, "7");
        verify(authorizer, never()).checkCaregiver(anyLong(), anyLong());
    }

    @Test
    void 체중_동기화는_삭제됐거나_없는_반려동물이면_404() {
        BigDecimal weight = new BigDecimal("5.250");
        when(mapper.updatePetWeight(PET, weight, "7")).thenReturn(0);

        assertCode(() -> service.syncWeight(PET, weight, 7L), ErrorCode.PET_NOT_FOUND);
    }

    private void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(code));
    }

    @Test
    void 등록에서_N이면_보낸_범위를_무시하고_체중으로_계산해_저장한다() {
        service.createPet(MBR, rangeCreate("N", kg("1"), kg("9")));

        Pet saved = insertedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("N");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.78");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.62");
    }

    @Test
    void 등록에서_N이면_범위를_안_보내도_된다() {
        service.createPet(MBR, rangeCreate("N", null, null));

        Pet saved = insertedPet();
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.78");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.62");
    }

    @Test
    void 등록에서_Y이면_보낸_범위를_그대로_저장한다() {
        service.createPet(MBR, rangeCreate("Y", kg("3"), kg("5")));

        Pet saved = insertedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("Y");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("5");
    }

    @Test
    void 등록에서_플래그를_안_보내면_Y로_저장한다() {
        service.createPet(MBR, rangeCreate(null, kg("3"), kg("5")));

        assertThat(insertedPet().getBdwgRngDrctInptYn()).isEqualTo("Y");
    }

    @Test
    void 등록에서_Y인데_범위가_없으면_400() {
        assertFields(() -> service.createPet(MBR, rangeCreate("Y", null, kg("5"))), "minBdwg", "maxBdwg");
        assertFields(() -> service.createPet(MBR, rangeCreate(null, kg("3"), null)), "minBdwg", "maxBdwg");
        verify(mapper, never()).insertPet(any());
    }

    @Test
    void 수정에서_N으로_바꾸면_현재_체중으로_다시_계산하고_보낸_범위는_무시한다() {
        service.updatePet(MBR, PET, rangeUpdate("N", null, kg("1"), kg("9")));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("N");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.78");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.62");
    }

    @Test
    void 수정에서_N과_체중을_같이_보내면_새_체중으로_계산한다() {
        service.updatePet(MBR, PET, rangeUpdate("N", kg("10"), null, null));

        Pet saved = updatedPet();
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("9.00");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("11.00");
    }

    @Test
    void 수정에서_Y이고_범위를_보내면_그대로_저장한다() {
        service.updatePet(MBR, PET, rangeUpdate("Y", null, kg("2"), kg("6")));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("Y");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("2");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("6");
    }

    @Test
    void 수정에서_Y로_바꾸며_범위를_안_보내면_기존_범위를_유지한다() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> autoPet());

        service.updatePet(MBR, PET, rangeUpdate("Y", null, null, null));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("Y");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.78");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.62");
    }

    @Test
    void 수정에서_체중만_보내면_현재_Y는_범위를_유지한다() {
        service.updatePet(MBR, PET, rangeUpdate(null, kg("10"), null, null));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("Y");
        assertThat(saved.getPetBdwg()).isEqualByComparingTo("10");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("5");
    }

    @Test
    void 수정에서_체중만_보내면_현재_N은_다시_계산한다() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> autoPet());

        service.updatePet(MBR, PET, rangeUpdate(null, kg("10"), null, null));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("N");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("9.00");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("11.00");
    }

    @Test
    void 수정에서_아무것도_안_보내면_현재_N의_범위를_다시_계산하지_않는다() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> {
            Pet pet = autoPet();
            pet.setMinBdwg(kg("3.1"));
            pet.setMaxBdwg(kg("4.9"));
            return pet;
        });

        service.updatePet(MBR, PET, rangeUpdate(null, null, null, null));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("N");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.1");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.9");
    }

    @Test
    void 수정에서_현재_N인데_플래그_없이_범위를_보내면_Y로_바꾸고_보낸_값을_저장한다() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> autoPet());

        service.updatePet(MBR, PET, rangeUpdate(null, null, kg("2"), kg("6")));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("Y");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("2");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("6");
    }

    @Test
    void 수정에서_N과_범위를_같이_보내면_범위를_무시하고_계산한다() {
        when(mapper.selectPet(PET)).thenAnswer(inv -> autoPet());

        service.updatePet(MBR, PET, rangeUpdate("N", null, kg("2"), kg("6")));

        Pet saved = updatedPet();
        assertThat(saved.getBdwgRngDrctInptYn()).isEqualTo("N");
        assertThat(saved.getMinBdwg()).isEqualByComparingTo("3.78");
        assertThat(saved.getMaxBdwg()).isEqualByComparingTo("4.62");
    }

    private Pet insertedPet() {
        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).insertPet(pet.capture());
        return pet.getValue();
    }

    private Pet updatedPet() {
        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        return pet.getValue();
    }

    /** 자동 계산('N') 중인 반려동물 : 체중 4.2, 범위 3.78~4.62 */
    private Pet autoPet() {
        Pet pet = storedPet();
        pet.setBdwgRngDrctInptYn("N");
        pet.setMinBdwg(kg("3.78"));
        pet.setMaxBdwg(kg("4.62"));
        return pet;
    }

    private PetCreateRequest rangeCreate(String yn, BigDecimal minBdwg, BigDecimal maxBdwg) {
        return new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE, true, kg("4.2"),
                null, minBdwg, maxBdwg, yn, WHITE, null, null, null, null);
    }

    private PetUpdateRequest rangeUpdate(String yn, BigDecimal petBdwg, BigDecimal minBdwg, BigDecimal maxBdwg) {
        return new PetUpdateRequest(null, null, null, null, null, null, null, null, petBdwg, null, minBdwg, maxBdwg,
                yn, null, null, null, null, null, null);
    }

    private PetCreateRequest create(Long vrtySn, String vrtyDrctInptNm, String algyCn, String dssCn) {
        return new PetCreateRequest("뭉치", DOG, vrtySn, vrtyDrctInptNm, LocalDate.of(2023, 5, 1), "", MALE, true,
                kg("4.2"), null, kg("3"), kg("5"), null, WHITE, null, null, algyCn, dssCn);
    }

    private PetUpdateRequest update(BigDecimal minBdwg, BigDecimal petBdwg, Boolean nutrYn, String algyCn,
            String dssCn) {
        return new PetUpdateRequest(null, null, null, null, null, null, null, nutrYn, petBdwg, null, minBdwg, null,
                null, null, null, null, algyCn, dssCn, null);
    }

    private PetCreateRequest colorCreate(String petSeCd, String colrCd) {
        return new PetCreateRequest("뭉치", petSeCd, 15L, null, LocalDate.of(2023, 5, 1), null, MALE, true,
                kg("4.2"), null, kg("3"), kg("5"), null, colrCd,
                DIRECT_COLOR.equals(colrCd) ? "얼룩덜룩" : null, null, null, null);
    }

    private PetUpdateRequest colorUpdate(String petSeCd, String colrCd) {
        return new PetUpdateRequest(null, petSeCd, null, null, null, null, null, null, null, null, null, null, null,
                colrCd, null, null, null, null, null);
    }

    private PetUpdateRequest goalUpdate(BigDecimal goalBdwg, Boolean goalBdwgReset) {
        return new PetUpdateRequest(null, null, null, null, null, null, null, null, null, goalBdwg, null, null,
                null, null, null, null, null, null, goalBdwgReset);
    }

    private Pet storedPet() {
        Pet pet = new Pet();
        pet.setPetSn(PET);
        pet.setPetNm("뭉치");
        pet.setPetSeCd(DOG);
        pet.setVrtySn(15L);
        pet.setPetGndrCd(MALE);
        pet.setPetBrdt(LocalDate.of(2023, 5, 1));
        pet.setPetBdwg(kg("4.2"));
        pet.setMinBdwg(kg("3"));
        pet.setMaxBdwg(kg("5"));
        pet.setBdwgRngDrctInptYn("Y");
        pet.setColrCd(WHITE);
        pet.setNutrYn("Y");
        pet.setGoalBdwg(kg("4"));
        return pet;
    }

    private BrupRel rel(String roleCd, String rprsPetYn) {
        BrupRel rel = new BrupRel();
        rel.setBrupRoleCd(roleCd);
        rel.setRprsPetYn(rprsPetYn);
        return rel;
    }

    private CaregiverSummary caregiver(Long mbrSn, String roleCd) {
        CaregiverSummary caregiver = new CaregiverSummary();
        caregiver.setMbrSn(mbrSn);
        caregiver.setBrupRoleCd(roleCd);
        return caregiver;
    }

    private List<CommonCodeSummary> codeList(String... comCds) {
        List<CommonCodeSummary> list = new ArrayList<>();
        for (String comCd : comCds) {
            CommonCodeSummary code = new CommonCodeSummary();
            code.setComCd(comCd);
            list.add(code);
        }
        return list;
    }

    private BigDecimal kg(String value) {
        return new BigDecimal(value);
    }

    @Test
    void 수정_서비스_검사_400은_틀린_칸_이름을_담는다() {
        assertFields(() -> service.updatePet(MBR, PET, colorUpdate(null, CAT_ONLY_COLOR)), "colrCd");
        assertFields(() -> service.updatePet(MBR, PET, new PetUpdateRequest(null, null, null, null, null, null,
                "PETC9999", null, null, null, null, null, null, null, null, null, null, null, null)), "petGndrCd");
        assertFields(() -> service.updatePet(MBR, PET, update(kg("6"), null, null, null, null)), "minBdwg",
                "maxBdwg");
        assertFields(() -> service.updatePet(MBR, PET, new PetUpdateRequest(null, null, 15L, "직접", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null)), "vrtySn", "vrtyDrctInptNm");
    }

    private static void assertFields(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String... fields) {
        assertThatThrownBy(call).isInstanceOfSatisfying(CustomException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
            assertThat(e.getFields()).containsExactly(fields);
        });
    }
}
