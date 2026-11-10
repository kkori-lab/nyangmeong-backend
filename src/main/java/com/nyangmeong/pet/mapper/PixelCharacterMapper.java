package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterItemSheet;
import com.nyangmeong.pet.domain.CharacterJobStatusHistory;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.AdminCharacterJobRow;
import com.nyangmeong.pet.dto.AdminCharacterJobSearchCond;
import com.nyangmeong.pet.dto.AdminCharacterJobStatusCount;
import com.nyangmeong.pet.dto.HeldItemSummary;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.dto.PixelCharacterSummary;

/**
 * [픽셀 캐릭터 Mapper]
 * - PET·BRUP_REL 은 조회만 (등록·수정은 반려동물 관리)
 */
@Mapper
public interface PixelCharacterMapper {

    /**
     * 회원의 양육 반려동물별 캐릭터 목록 조회
     */
    List<PixelCharacterSummary> selectList(Long mbrSn);

    /**
     * 캐릭터 조회
     */
    PixelCharacter selectPixelCharacter(Long pixelCharSn);

    /**
     * 반려동물의 캐릭터 조회
     */
    PixelCharacter selectByPetSn(Long petSn);

    /**
     * 캐릭터 등록
     */
    void insertPixelCharacter(PixelCharacter character);

    /**
     * 오류 확인 중(기본 캐릭터 대체) 여부 변경
     */
    int updateBscCharSbstYn(@Param("pixelCharSn") Long pixelCharSn, @Param("bscCharSbstYn") String bscCharSbstYn,
            @Param("actorId") String actorId);

    /**
     * 착용 모습 대표 이미지 저장·삭제 : NULL이면 삭제
     */
    int updateWearImage(@Param("pixelCharSn") Long pixelCharSn, @Param("wearImgFileSn") Long wearImgFileSn,
            @Param("actorId") String actorId);

    /**
     * 맨몸 시트·배치값 저장
     */
    int updateBodySheet(@Param("pixelCharSn") Long pixelCharSn, @Param("bodySheetFileSn") Long bodySheetFileSn,
            @Param("sheetScale") Double sheetScale, @Param("sheetOriginX") Double sheetOriginX,
            @Param("sheetOriginY") Double sheetOriginY, @Param("actorId") String actorId);

    /**
     * 반려동물의 픽셀 캐릭터 삭제
     */
    int updateDeleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);

    /**
     * 반려동물 캐릭터의 보유 아이템 삭제 (착용 해제 포함)
     */
    int updateHeldItemDeleteByPetSn(Long petSn);

    /**
     * 반려동물의 캐릭터 생성 작업 삭제 (없으면 0)
     */
    int updateJobDeleteByPetSn(Long petSn);

    /**
     * 반려동물 캐릭터의 사진 삭제 (없으면 0)
     */
    int updatePhotoDeleteByPetSn(Long petSn);

    /**
     * 반려동물 캐릭터의 착용 시트 삭제 (없으면 0)
     */
    int updateItemSheetDeleteByPetSn(Long petSn);

    /**
     * 생성 작업 조회
     */
    CharacterCreationJob selectJob(Long charCrtJobSn);

    /**
     * 멱등 요청 ID로 생성 작업 조회
     */
    CharacterCreationJob selectJobByIdmptDmndId(String idmptDmndId);

    /**
     * 반려동물의 최근 생성 작업 조회
     */
    CharacterCreationJob selectLatestJob(Long petSn);

    /**
     * 생성 작업 등록
     */
    void insertJob(CharacterCreationJob job);

    /**
     * 생성 시작 처리
     */
    int updateJobStarted(@Param("charCrtJobSn") Long charCrtJobSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 생성 종료 처리
     */
    int updateJobFinished(@Param("charCrtJobSn") Long charCrtJobSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("failRsn") String failRsn);

    /**
     * 시작 뒤 제한 시간을 넘긴 생성 작업 조회
     */
    List<Long> selectTimedOutJobs(@Param("crtJobSttsCd") String crtJobSttsCd, @Param("minutes") int minutes);

    /**
     * 생성 작업에 오류 문의 연결
     */
    int updateJobInquiry(@Param("charCrtJobSn") Long charCrtJobSn, @Param("otoInqrySn") Long otoInqrySn);

    /**
     * 생성 작업 상태 이력 등록
     */
    void insertJobStatusHistory(CharacterJobStatusHistory history);

    /**
     * 캐릭터 사진 등록
     */
    void insertPhoto(CharacterPhoto photo);

    /**
     * 생성 작업의 사진 조회
     */
    CharacterPhoto selectJobPhoto(@Param("charCrtJobSn") Long charCrtJobSn, @Param("photoRoleCd") String photoRoleCd);

    /**
     * 현재 표시 중인 결과 사진 조회
     */
    CharacterPhoto selectCurrentPhoto(Long pixelCharSn);

    /**
     * 캐릭터 사진 단건 조회
     */
    CharacterPhoto selectPhoto(Long charPhotoSn);

    /**
     * 현재 표시 사진 해제
     */
    int updatePhotoHidden(Long pixelCharSn);

    /**
     * 보관함 아이템 목록 조회
     */
    List<HeldItemSummary> selectHeldItems(Long pixelCharSn);

    /**
     * 보유 아이템 전부 착용 해제
     */
    int updateUnequipAll(Long pixelCharSn);

    /**
     * 보유 아이템 착용
     */
    int updateEquip(@Param("pixelCharSn") Long pixelCharSn, @Param("hldItemSns") List<Long> hldItemSns);

    /**
     * 착용 시트 저장 (다시 받으면 교체)
     */
    int upsertItemSheet(@Param("pixelCharSn") Long pixelCharSn, @Param("itemSn") Long itemSn,
            @Param("atchFileSn") Long atchFileSn);

    /**
     * 굽기 실패 표시 (이미 구운 시트는 유지)
     */
    int upsertItemSheetFailure(@Param("pixelCharSn") Long pixelCharSn, @Param("itemSn") Long itemSn);

    /**
     * 캐릭터의 착용 시트 전부 삭제
     */
    int updateItemSheetDelete(Long pixelCharSn);

    /**
     * 아이템의 착용 시트 전부 삭제 (굽기 지시 변경 -> 다시 굽기 대상)
     */
    int updateItemSheetDeleteByItemSn(Long itemSn);

    /**
     * 아이템을 아직 안 구운 캐릭터 번호 조회 (맨몸 시트 있음, 착용 시트·굽기 실패 표시 없음)
     */
    List<Long> selectBakeTargets(Long itemSn);

    /**
     * 아이템별 착용 시트 조회
     */
    List<CharacterItemSheet> selectItemSheets(@Param("pixelCharSn") Long pixelCharSn,
            @Param("itemSns") List<Long> itemSns);

    /**
     * 캐릭터의 착용 시트 파일 여부
     */
    int countItemSheetFile(@Param("pixelCharSn") Long pixelCharSn, @Param("atchFileSn") Long atchFileSn);

    /**
     * 캐릭터 생성용 반려동물 정보 조회
     */
    PetProfileSummary selectPetProfile(Long petSn);

    /**
     * 관리자 캐릭터 생성 작업 상태별 건수 (있는 상태만, SCR-ADM-082)
     */
    List<AdminCharacterJobStatusCount> selectAdminJobStatusCounts();

    /**
     * 관리자 캐릭터 생성 작업 기간 내 상태별 건수 (있는 상태만, 상태 필터 무관, SCR-ADM-082)
     */
    List<AdminCharacterJobStatusCount> selectAdminJobStatusCountsInPeriod(
            @Param("cond") AdminCharacterJobSearchCond cond);

    /**
     * 관리자 캐릭터 생성 작업 목록 (기간·상태 필터, 최신순, SCR-ADM-082)
     */
    List<AdminCharacterJobRow> selectAdminJobList(@Param("cond") AdminCharacterJobSearchCond cond);
}
