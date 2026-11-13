package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.CharacterGenerationRequest;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.domain.CharacterJobStatusHistory;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PhotoRole;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.CharacterSuccessRequest;
import com.nyangmeong.pet.dto.CharacterSuccessResponse;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [캐릭터 생성 작업 상태 처리]
 * - 시작·완료·실패 각각 별도 트랜잭션 : 생성은 Python 보고(reportSuccess·reportFailure), 재생성은 CharacterCreationWorker
 * - 완료 시 결과 저장 + 오류 확인 중 해제 + 캐릭터 알림 (FN-MYP-010-09·23)
 */
@Service
@RequiredArgsConstructor
public class CharacterCreationService {

    static final String SYSTEM = "SYSTEM";
    static final String MVMN_PATH = "/mypage/characters";
    private static final int FAIL_RSN_MAX = 1000;
    /** Python 제한 180초 × 2회 + 여유 */
    private static final int TIMEOUT_MINUTES = 10;

    private final PixelCharacterMapper pixelCharacterMapper;
    private final FileStore fileStore;
    private final EventPublisher eventPublisher;
    private final OwnerAuthorizer ownerAuthorizer;

    /**
     * 생성 시작
     */
    @Transactional
    public CharacterGenerationRequest start(Long charCrtJobSn) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.REQUESTED.is(job.getCrtJobSttsCd())) {
            return null;
        }
        if (pixelCharacterMapper.updateJobStarted(charCrtJobSn, CharacterJobStatus.REQUESTED.getCode(),
                CharacterJobStatus.CREATING.getCode()) == 0) {
            return null;
        }
        insertHistory(charCrtJobSn, CharacterJobStatus.REQUESTED, CharacterJobStatus.CREATING, null);

        CharacterPhoto original = pixelCharacterMapper.selectJobPhoto(charCrtJobSn, PhotoRole.ORIGINAL.getCode());
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(job.getPetSn());
        if (original == null || pet == null) {
            throw new IllegalStateException("생성 입력 없음 charCrtJobSn=" + charCrtJobSn);
        }
        byte[] photo = fileStore.load(original.getAtchFileSn()).content();
        return new CharacterGenerationRequest(photo, pet.getPetSeCd(), pet.getVrtyNm(), pet.getColrNm(), pet.getChrcCn());
    }

    /**
     * 생성 완료
     */
    @Transactional
    public void complete(Long charCrtJobSn, byte[] image) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.CREATING.is(job.getCrtJobSttsCd())) {
            return;
        }
        PixelCharacter character = pixelCharacterMapper.selectByPetSn(job.getPetSn());
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(job.getPetSn());
        if (character == null || pet == null) {
            throw new IllegalStateException("삭제된 반려동물 charCrtJobSn=" + charCrtJobSn);
        }

        saveResult(charCrtJobSn, character, pet, image);
    }

    /**
     * 생성 성공 보고
     * - 이미 완료 -> 같은 번호, 시간 초과 등으로 실패한 뒤의 보고는 거절
     */
    @Transactional
    public CharacterSuccessResponse reportSuccess(Long mbrSn, Long charCrtJobSn, CharacterSuccessRequest req) {
        CharacterCreationJob job = getOwnedJob(mbrSn, charCrtJobSn);
        PixelCharacter character = pixelCharacterMapper.selectByPetSn(job.getPetSn());
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(job.getPetSn());
        if (character == null || pet == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        if (CharacterJobStatus.COMPLETED.is(job.getCrtJobSttsCd())) {
            return new CharacterSuccessResponse(character.getPixelCharSn());
        }
        if (!CharacterJobStatus.CREATING.is(job.getCrtJobSttsCd())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        byte[] image = CharacterSheetService.readBytes(req.image());
        if (!CharacterSpec.matches(image)) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        byte[] sheet = CharacterSheetService.readSheet(req.sheet());

        saveResult(charCrtJobSn, character, pet, image);
        Long sheetFileSn = fileStore.store(sheet, FileExtension.PNG, "sheet-" + character.getPixelCharSn() + ".png",
                FileCategory.PET_SHEET, FileOwner.pet(character.getMbrSn(), character.getPetSn()), SYSTEM);
        pixelCharacterMapper.updateBodySheet(character.getPixelCharSn(), sheetFileSn, req.scale(), req.originX(),
                req.originY(), SYSTEM);
        pixelCharacterMapper.updateItemSheetDelete(character.getPixelCharSn());
        pixelCharacterMapper.updateWearImage(character.getPixelCharSn(), null, SYSTEM);
        return new CharacterSuccessResponse(character.getPixelCharSn());
    }

    /**
     * 생성 실패 보고
     */
    @Transactional
    public void reportFailure(Long mbrSn, Long charCrtJobSn, String failRsn) {
        getOwnedJob(mbrSn, charCrtJobSn);
        fail(charCrtJobSn, failRsn);
    }

    /**
     * 시간 초과 작업 조회
     */
    @Transactional(readOnly = true)
    public List<Long> searchTimedOutJobs() {
        return pixelCharacterMapper.selectTimedOutJobs(CharacterJobStatus.CREATING.getCode(), TIMEOUT_MINUTES);
    }

    /**
     * 결과 저장·완료 처리·알림
     */
    private void saveResult(Long charCrtJobSn, PixelCharacter character, PetProfileSummary pet, byte[] image) {
        Long atchFileSn = fileStore.store(image, FileExtension.PNG,
                "character-" + character.getPixelCharSn() + ".png", FileCategory.PET_CHARACTER,
                FileOwner.pet(character.getMbrSn(), character.getPetSn()), SYSTEM);
        pixelCharacterMapper.updatePhotoHidden(character.getPixelCharSn());
        CharacterPhoto result = new CharacterPhoto();
        result.setPixelCharSn(character.getPixelCharSn());
        result.setCharCrtJobSn(charCrtJobSn);
        result.setAtchFileSn(atchFileSn);
        result.setPhotoRoleCd(PhotoRole.RESULT.getCode());
        result.setSortSeq(0);
        result.setNowIndctYn("Y");
        pixelCharacterMapper.insertPhoto(result);

        pixelCharacterMapper.updateJobFinished(charCrtJobSn, List.of(CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.COMPLETED.getCode(), null);
        insertHistory(charCrtJobSn, CharacterJobStatus.CREATING, CharacterJobStatus.COMPLETED, null);
        pixelCharacterMapper.updateBscCharSbstYn(character.getPixelCharSn(), "N", SYSTEM);

        eventPublisher.publish(new NotificationEvent(character.getMbrSn(), NotificationType.CHARACTER,
                pet.getPetNm() + "의 캐릭터가 완성됐어요", null, MVMN_PATH));
    }

    /**
     * 생성 실패
     */
    @Transactional
    public void fail(Long charCrtJobSn, String reason) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.isInProgress(job.getCrtJobSttsCd())) {
            return;
        }
        String failRsn = truncate(reason);
        int updated = pixelCharacterMapper.updateJobFinished(charCrtJobSn,
                List.of(CharacterJobStatus.REQUESTED.getCode(), CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.FAILED.getCode(), failRsn);
        if (updated > 0) {
            CharacterJobStatus before = CharacterJobStatus.REQUESTED.is(job.getCrtJobSttsCd())
                    ? CharacterJobStatus.REQUESTED
                    : CharacterJobStatus.CREATING;
            insertHistory(charCrtJobSn, before, CharacterJobStatus.FAILED, failRsn);
        }
    }

    /**
     * 본인 작업 조회
     */
    private CharacterCreationJob getOwnedJob(Long mbrSn, Long charCrtJobSn) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        ownerAuthorizer.checkOwner(mbrSn, job.getMbrSn());
        return job;
    }

    /**
     * 상태 이력 등록
     */
    private void insertHistory(Long charCrtJobSn, CharacterJobStatus before, CharacterJobStatus after, String reason) {
        CharacterJobStatusHistory history = new CharacterJobStatusHistory();
        history.setCharCrtJobSn(charCrtJobSn);
        history.setBfrSttsCd(before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(reason);
        pixelCharacterMapper.insertJobStatusHistory(history);
    }

    /**
     * 실패 사유 길이 제한
     */
    private String truncate(String reason) {
        String value = reason == null || reason.isBlank() ? "알 수 없는 오류" : reason;
        return value.length() > FAIL_RSN_MAX ? value.substring(0, FAIL_RSN_MAX) : value;
    }
}
