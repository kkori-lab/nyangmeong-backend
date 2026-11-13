package com.nyangmeong.community.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.BoardType;
import com.nyangmeong.community.domain.CareCostUnit;
import com.nyangmeong.community.domain.CareRecruitment;
import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentPet;
import com.nyangmeong.community.domain.CareRecruitmentShape;
import com.nyangmeong.community.domain.CareRecruitmentStatus;
import com.nyangmeong.community.domain.CareRecruitmentStatusHistory;
import com.nyangmeong.community.domain.CareShape;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.domain.PetSize;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.CareRecruitmentCreateRequest;
import com.nyangmeong.community.dto.CareRecruitmentPetItem;
import com.nyangmeong.community.dto.CareRecruitmentPetRequestItem;
import com.nyangmeong.community.dto.CareRecruitmentResponse;
import com.nyangmeong.community.dto.CareRecruitmentSearchCond;
import com.nyangmeong.community.dto.CareRecruitmentSummary;
import com.nyangmeong.community.dto.CareRecruitmentUpdateRequest;
import com.nyangmeong.community.dto.MinimumWageResponse;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.code.RegionService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.pet.domain.PetSe;
import com.nyangmeong.pet.service.BreedService;

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 모집 서비스]
 * - SCR-CMU-004 목록·상세·등록·수정·삭제, 채팅하기는 ChatRoomService
 * - 게시글(PST, 펫돌봄 게시판)과 모집(PET_PRTC_RCRT)이 1:1, 좋아요·신고는 /api/posts, /api/reports 와 같이 쓴다
 * - 돌봄 지역은 장소 좌표로 서버가 계산하고 전북 밖이면 거절한다, 상세 주소는 암호화해 저장하고 응답에 담지 않는다
 */
@Service
@RequiredArgsConstructor
public class CareRecruitmentService {

    static final int PAGE_SIZE = 10;

    private final CareRecruitmentMapper careRecruitmentMapper;
    private final PostMapper postMapper;
    private final FileStore fileStore;
    private final OwnerAuthorizer ownerAuthorizer;
    private final SystemSettingService systemSettingService;
    private final FieldCryptoService fieldCryptoService;
    private final RegionService regionService;
    private final BreedService breedService;

    /**
     * 목록 조회 (최신순 10건, 반려동물·돌봄 형태는 목록 조회 뒤 한 번에 채움)
     */
    @Transactional(readOnly = true)
    public PageResponse<CareRecruitmentSummary> search(Long mbrSn, CareRecruitmentSearchCond cond, PageQuery pq) {
        String rcrtSttsCd = blankToNull(cond.getRcrtSttsCd());
        if (rcrtSttsCd != null && !CareRecruitmentStatus.exists(rcrtSttsCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String petSeCd = blankToNull(cond.getPetSeCd());
        if (petSeCd != null) {
            breedService.checkPetSe(petSeCd);
        }
        PetSize size = null;
        String petSizeCd = blankToNull(cond.getPetSizeCd());
        if (petSizeCd != null) {
            size = PetSize.from(petSizeCd);
            if (size == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        /** 고양이만 고르면 크기 조건은 적용하지 않음 */
        boolean sizeFilter = size != null && !PetSe.CAT.is(petSeCd);
        LocalDate bgngDe = cond.getPrtcBgngDe();
        LocalDate endDe = cond.getPrtcEndDe();
        if (bgngDe != null && endDe != null && endDe.isBefore(bgngDe)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<CareRecruitmentSummary> rows = careRecruitmentMapper.selectList(mbrSn, PostStatus.NORMAL.getCode(),
                rcrtSttsCd, cond.isMineYn(), cond.getRgnSn(), bgngDe == null ? null : bgngDe.atStartOfDay(),
                endDe == null ? null : endDe.plusDays(1).atStartOfDay(), petSeCd, PetSe.DOG.getCode(), sizeFilter,
                sizeFilter ? size.getMinBdwg() : null, sizeFilter ? size.getMaxBdwg() : null);
        PageInfo<CareRecruitmentSummary> pageInfo = new PageInfo<>(rows);
        fillChildren(rows);
        return PageResponse.of(pageInfo);
    }

    /**
     * 최저시급 조회 (관리자 설정을 매번 읽어 바꾸면 바로 반영)
     */
    @Transactional(readOnly = true)
    public MinimumWageResponse getMinimumWage() {
        SystemSetting setting = systemSettingService.get();
        return new MinimumWageResponse(setting.getLowstHrwgAplcnYr(), setting.getLowstHrwgAmt());
    }

    /**
     * 상세 조회 (조회수는 같은 회원 하루 한 번)
     */
    @Transactional
    public CareRecruitmentResponse getRecruitment(Long mbrSn, Long petPrtcRcrtSn) {
        CareRecruitmentDetail detail = getDetailOrThrow(petPrtcRcrtSn);
        /** 작성자 본인 조회는 조회수에 넣지 않는다. 같은 회원·같은 날 이력이 이미 있으면 0건 */
        if (!mbrSn.equals(detail.getMbrSn()) && postMapper.insertViewIgnore(detail.getPstSn(), mbrSn) > 0) {
            postMapper.updateInqCnt(detail.getPstSn());
            detail.setInqCnt(detail.getInqCnt() + 1);
        }
        return toResponse(mbrSn, detail);
    }

    /**
     * 모집 등록
     */
    @Transactional
    public CareRecruitmentResponse create(Long mbrSn, CareRecruitmentCreateRequest req) {
        boolean schdlCnsltn = Boolean.TRUE.equals(req.schdlCnsltnPsbltyYn());
        if (!Boolean.TRUE.equals(req.utztnRulAgreYn())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<CareRecruitmentPet> pets = toPets(req.pets());
        List<String> shapes = checkShapes(req.prtcShpCds());
        checkCost(req.cstUnitCd(), req.cstAmt());
        checkSchedule(schdlCnsltn, req.prtcBgngDt(), req.prtcEndDt());
        Long rgnSn = findRegionOrThrow(req.prtcPlcLat(), req.prtcPlcLot());
        List<MultipartFile> images = attached(req.images());
        if (images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        Post post = new Post();
        post.setMbrSn(mbrSn);
        post.setBbsCd(BoardType.CARE.getCode());
        post.setPstSttsCd(PostStatus.NORMAL.getCode());
        post.setPstTtl(req.pstTtl());
        post.setPstCn(req.pstCn());
        post.setMdfrId(actorId(mbrSn));
        postMapper.insertPost(post);
        insertPostHistory(post.getPstSn(), mbrSn, null, PostStatus.NORMAL);

        CareRecruitment recruitment = new CareRecruitment();
        recruitment.setPstSn(post.getPstSn());
        recruitment.setRgnSn(rgnSn);
        recruitment.setRcrtSttsCd(CareRecruitmentStatus.RECRUITING.getCode());
        recruitment.setCstUnitCd(req.cstUnitCd());
        recruitment.setCstAmt(req.cstAmt());
        recruitment.setRlsRgnNm(req.rlsRgnNm());
        recruitment.setPrtcPlcDaddrEnc(fieldCryptoService.encrypt(req.prtcPlcDaddr()));
        recruitment.setPrtcBgngDt(req.prtcBgngDt());
        recruitment.setPrtcEndDt(req.prtcEndDt());
        recruitment.setSchdlCnsltnPsbltyYn(schdlCnsltn ? "Y" : "N");
        recruitment.setUtztnRulAgreYn("Y");
        recruitment.setPrtcPlcLat(req.prtcPlcLat());
        recruitment.setPrtcPlcLot(req.prtcPlcLot());
        careRecruitmentMapper.insertRecruitment(recruitment);
        insertRecruitmentHistory(recruitment.getPetPrtcRcrtSn(), mbrSn, null, CareRecruitmentStatus.RECRUITING);

        insertChildren(recruitment.getPetPrtcRcrtSn(), pets, shapes);
        int sortSeq = 0;
        for (MultipartFile image : images) {
            insertImage(post.getPstSn(), fileStore.store(image, FileCategory.COMMUNITY_POST, FileOwner.member(mbrSn), actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getDetailOrThrow(recruitment.getPetPrtcRcrtSn()));
    }

    /**
     * 모집 수정 (작성자, 모집중만, 보낸 필드만)
     */
    @Transactional
    public CareRecruitmentResponse update(Long mbrSn, Long petPrtcRcrtSn, CareRecruitmentUpdateRequest req) {
        CareRecruitmentDetail detail = getDetailOrThrow(petPrtcRcrtSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (!CareRecruitmentStatus.RECRUITING.is(detail.getRcrtSttsCd())) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_EDITABLE);
        }
        if ((req.prtcPlcLat() == null) != (req.prtcPlcLot() == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        /** 보내지 않은 항목은 지금 값을 그대로 두고 합친 값으로 검사 */
        boolean schdlCnsltn = req.schdlCnsltnPsbltyYn() != null ? req.schdlCnsltnPsbltyYn()
                : "Y".equals(detail.getSchdlCnsltnPsbltyYn());
        String cstUnitCd = req.cstUnitCd() != null ? req.cstUnitCd() : detail.getCstUnitCd();
        Integer cstAmt = req.cstAmt() != null ? req.cstAmt() : detail.getCstAmt();
        /** 일정 협의 가능을 보내면서 일정을 둘 다 보내지 않으면 기존 일정을 비움(일정 미정으로 바꾸기) */
        boolean clearSchedule = Boolean.TRUE.equals(req.schdlCnsltnPsbltyYn()) && req.prtcBgngDt() == null
                && req.prtcEndDt() == null;
        LocalDateTime bgngDt = clearSchedule ? null
                : req.prtcBgngDt() != null ? req.prtcBgngDt() : detail.getPrtcBgngDt();
        LocalDateTime endDt = clearSchedule ? null
                : req.prtcEndDt() != null ? req.prtcEndDt() : detail.getPrtcEndDt();
        checkCost(cstUnitCd, cstAmt);
        checkSchedule(schdlCnsltn, bgngDt, endDt);
        List<CareRecruitmentPet> pets = req.pets() == null ? null : toPets(req.pets());
        List<String> shapes = req.prtcShpCds() == null ? null : checkShapes(req.prtcShpCds());

        boolean placeChanged = req.prtcPlcLat() != null;
        Long rgnSn = placeChanged ? findRegionOrThrow(req.prtcPlcLat(), req.prtcPlcLot()) : detail.getRgnSn();

        List<MultipartFile> images = attached(req.images());
        List<PostImage> remaining = new ArrayList<>(postMapper.selectImages(detail.getPstSn()));
        List<Long> removed = new ArrayList<>();
        if (req.keepAtchFileSns() != null) {
            for (PostImage image : remaining) {
                if (!req.keepAtchFileSns().contains(image.getAtchFileSn())) {
                    removed.add(image.getAtchFileSn());
                }
            }
            remaining.removeIf(image -> removed.contains(image.getAtchFileSn()));
        }
        if (remaining.size() + images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        /** 게시판은 바꾸지 않음(bbsCd null), 제목·본문은 보낸 것만 */
        if (postMapper.updatePost(detail.getPstSn(), null, req.pstTtl(), req.pstCn(), actorId(mbrSn),
                PostStatus.NORMAL.getCode()) == 0) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        CareRecruitment recruitment = new CareRecruitment();
        recruitment.setPetPrtcRcrtSn(petPrtcRcrtSn);
        recruitment.setRgnSn(rgnSn);
        recruitment.setCstUnitCd(cstUnitCd);
        recruitment.setCstAmt(cstAmt);
        recruitment.setRlsRgnNm(req.rlsRgnNm() != null ? req.rlsRgnNm() : detail.getRlsRgnNm());
        recruitment.setPrtcPlcDaddrEnc(req.prtcPlcDaddr() != null ? fieldCryptoService.encrypt(req.prtcPlcDaddr())
                : detail.getPrtcPlcDaddrEnc());
        recruitment.setPrtcBgngDt(bgngDt);
        recruitment.setPrtcEndDt(endDt);
        recruitment.setSchdlCnsltnPsbltyYn(schdlCnsltn ? "Y" : "N");
        recruitment.setPrtcPlcLat(placeChanged ? req.prtcPlcLat() : detail.getPrtcPlcLat());
        recruitment.setPrtcPlcLot(placeChanged ? req.prtcPlcLot() : detail.getPrtcPlcLot());
        if (careRecruitmentMapper.updateRecruitment(recruitment,
                CareRecruitmentStatus.RECRUITING.getCode()) == 0) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_EDITABLE);
        }

        if (pets != null) {
            careRecruitmentMapper.deletePets(petPrtcRcrtSn);
        }
        if (shapes != null) {
            careRecruitmentMapper.deleteShapes(petPrtcRcrtSn);
        }
        insertChildren(petPrtcRcrtSn, pets == null ? List.of() : pets, shapes == null ? List.of() : shapes);
        if (!removed.isEmpty()) {
            postMapper.updateImagesDeleted(detail.getPstSn(), removed, actorId(mbrSn));
        }
        int sortSeq = remaining.stream().mapToInt(PostImage::getSortSeq).max().orElse(-1) + 1;
        for (MultipartFile image : images) {
            insertImage(detail.getPstSn(), fileStore.store(image, FileCategory.COMMUNITY_POST, FileOwner.member(mbrSn), actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getDetailOrThrow(petPrtcRcrtSn));
    }

    /**
     * 모집 삭제 (작성자, 상태 무관. 채팅·매칭 기록은 그대로 둠)
     */
    @Transactional
    public void delete(Long mbrSn, Long petPrtcRcrtSn) {
        CareRecruitmentDetail detail = getDetailOrThrow(petPrtcRcrtSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (postMapper.updateStatus(detail.getPstSn(), PostStatus.NORMAL.getCode(), PostStatus.DELETED.getCode(),
                actorId(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        insertPostHistory(detail.getPstSn(), mbrSn, PostStatus.NORMAL, PostStatus.DELETED);
    }

    /**
     * 정상 게시글의 모집 조회
     */
    private CareRecruitmentDetail getDetailOrThrow(Long petPrtcRcrtSn) {
        CareRecruitmentDetail detail = careRecruitmentMapper.selectDetail(petPrtcRcrtSn,
                PostStatus.NORMAL.getCode());
        if (detail == null) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 반려동물 요청 항목 검사와 저장용 변환 (구분 중복 불가, 품종은 마스터·직접입력 중 하나, 강아지는 크기 필수)
     */
    private List<CareRecruitmentPet> toPets(List<CareRecruitmentPetRequestItem> items) {
        Set<String> seen = new HashSet<>();
        List<CareRecruitmentPet> pets = new ArrayList<>();
        for (CareRecruitmentPetRequestItem item : items) {
            breedService.checkPetSe(item.petSeCd());
            if (!seen.add(item.petSeCd())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            String drctNm = blankToNull(item.vrtyDrctInptNm());
            if ((item.vrtySn() == null) == (drctNm == null)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (item.vrtySn() != null) {
                breedService.getBreedOrThrow(item.vrtySn(), item.petSeCd());
            }
            CareRecruitmentPet pet = new CareRecruitmentPet();
            pet.setPetSeCd(item.petSeCd());
            pet.setVrtySn(item.vrtySn());
            pet.setVrtyDrctInptNm(drctNm);
            pet.setPetBdwg(BigDecimal.ZERO);
            if (PetSe.DOG.is(item.petSeCd())) {
                PetSize size = PetSize.from(item.petSizeCd());
                if (size == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }
                pet.setPetBdwg(size.getBdwg());
            }
            pets.add(pet);
        }
        return pets;
    }

    /**
     * 돌봄 형태 코드 검사 (1개 이상, 중복은 하나로)
     */
    private List<String> checkShapes(List<String> shapes) {
        if (shapes == null || shapes.isEmpty() || !shapes.stream().allMatch(CareShape::exists)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return shapes.stream().distinct().toList();
    }

    /**
     * 비용 검사 (시급은 최저시급 이상, 건당은 비교하지 않음)
     */
    private void checkCost(String cstUnitCd, Integer cstAmt) {
        if (!CareCostUnit.exists(cstUnitCd) || cstAmt == null || cstAmt <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (CareCostUnit.HOURLY.is(cstUnitCd) && cstAmt < systemSettingService.get().getLowstHrwgAmt()) {
            throw new CustomException(ErrorCode.CARE_MIN_WAGE_NOT_MET);
        }
    }

    /**
     * 일정 검사 (일정 협의가 아니면 필수, 입력했으면 30분 단위·종료가 시작보다 뒤)
     */
    private void checkSchedule(boolean schdlCnsltn, LocalDateTime bgngDt, LocalDateTime endDt) {
        if ((bgngDt == null) != (endDt == null) || (!schdlCnsltn && bgngDt == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (bgngDt == null) {
            return;
        }
        if (!endDt.isAfter(bgngDt) || !isHalfHour(bgngDt) || !isHalfHour(endDt)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean isHalfHour(LocalDateTime dateTime) {
        return dateTime.getMinute() % 30 == 0 && dateTime.getSecond() == 0 && dateTime.getNano() == 0;
    }

    /**
     * 장소 좌표의 전북 지역 번호
     */
    private Long findRegionOrThrow(BigDecimal lat, BigDecimal lot) {
        Long rgnSn = regionService.findJeonbukRgnSn(lat, lot);
        if (rgnSn == null) {
            throw new CustomException(ErrorCode.CARE_REGION_NOT_ALLOWED);
        }
        return rgnSn;
    }

    private void insertChildren(Long petPrtcRcrtSn, List<CareRecruitmentPet> pets, List<String> shapes) {
        for (CareRecruitmentPet pet : pets) {
            pet.setPetPrtcRcrtSn(petPrtcRcrtSn);
            careRecruitmentMapper.insertPet(pet);
        }
        for (String shapeCd : shapes) {
            CareRecruitmentShape shape = new CareRecruitmentShape();
            shape.setPetPrtcRcrtSn(petPrtcRcrtSn);
            shape.setPrtcShpCd(shapeCd);
            careRecruitmentMapper.insertShape(shape);
        }
    }

    /**
     * 목록 항목에 반려동물·돌봄 형태 채우기
     */
    private void fillChildren(List<CareRecruitmentSummary> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> sns = rows.stream().map(CareRecruitmentSummary::getPetPrtcRcrtSn).toList();
        Map<Long, List<CareRecruitmentPetItem>> pets = careRecruitmentMapper.selectPets(sns).stream()
                .collect(Collectors.groupingBy(CareRecruitmentPet::getPetPrtcRcrtSn,
                        Collectors.mapping(CareRecruitmentPetItem::from, Collectors.toList())));
        Map<Long, List<String>> shapes = careRecruitmentMapper.selectShapes(sns).stream()
                .collect(Collectors.groupingBy(CareRecruitmentShape::getPetPrtcRcrtSn,
                        Collectors.mapping(CareRecruitmentShape::getPrtcShpCd, Collectors.toList())));
        for (CareRecruitmentSummary row : rows) {
            row.setPets(pets.getOrDefault(row.getPetPrtcRcrtSn(), List.of()));
            row.setPrtcShpCds(shapes.getOrDefault(row.getPetPrtcRcrtSn(), List.of()));
        }
    }

    private CareRecruitmentResponse toResponse(Long mbrSn, CareRecruitmentDetail detail) {
        List<Long> sns = List.of(detail.getPetPrtcRcrtSn());
        List<CareRecruitmentPetItem> pets = careRecruitmentMapper.selectPets(sns).stream()
                .map(CareRecruitmentPetItem::from).toList();
        List<String> shapes = careRecruitmentMapper.selectShapes(sns).stream()
                .map(CareRecruitmentShape::getPrtcShpCd).toList();
        List<PostImageSummary> imgs = postMapper.selectImages(detail.getPstSn()).stream()
                .map(PostImageSummary::from).toList();
        return CareRecruitmentResponse.of(detail, pets, shapes, imgs, postMapper.existsLike(detail.getPstSn(), mbrSn),
                mbrSn.equals(detail.getMbrSn()),
                careRecruitmentMapper.existsChatting(detail.getPstSn(), mbrSn, ChatParticipantStatus.JOINED.getCode()),
                matchedSitterNcnm(detail));
    }

    /**
     * 매칭 완료·거래 완료인 모집글의 확정된 돌보미 닉네임 (그 밖에는 null)
     */
    private String matchedSitterNcnm(CareRecruitmentDetail detail) {
        boolean matched = CareRecruitmentStatus.MATCHED.is(detail.getRcrtSttsCd())
                || CareRecruitmentStatus.COMPLETED.is(detail.getRcrtSttsCd());
        if (!matched || detail.getCfmtnPetPrtcMtchSn() == null) {
            return null;
        }
        return careRecruitmentMapper.selectMatchedSitterNcnm(detail.getCfmtnPetPrtcMtchSn());
    }

    private void insertPostHistory(Long pstSn, Long mbrSn, PostStatus before, PostStatus after) {
        PostStatusHistory history = new PostStatusHistory();
        history.setPstSn(pstSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        postMapper.insertStatusHistory(history);
    }

    private void insertRecruitmentHistory(Long petPrtcRcrtSn, Long mbrSn, CareRecruitmentStatus before,
            CareRecruitmentStatus after) {
        CareRecruitmentStatusHistory history = new CareRecruitmentStatusHistory();
        history.setPetPrtcRcrtSn(petPrtcRcrtSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        careRecruitmentMapper.insertStatusHistory(history);
    }

    private void insertImage(Long pstSn, Long atchFileSn, int sortSeq, Long mbrSn) {
        PostImage image = new PostImage();
        image.setPstSn(pstSn);
        image.setAtchFileSn(atchFileSn);
        image.setSortSeq(sortSeq);
        image.setRgtrId(actorId(mbrSn));
        image.setMdfrId(actorId(mbrSn));
        postMapper.insertImage(image);
    }

    /**
     * 선택하지 않은 빈 파일 파트를 뺀 이미지 목록
     */
    private List<MultipartFile> attached(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    }

    private String actorId(Long mbrSn) {
        return String.valueOf(mbrSn);
    }

    private int maxImages() {
        return systemSettingService.get().getPstImgMaxCnt().intValue();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
