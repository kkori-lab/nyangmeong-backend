package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.Item;
import com.nyangmeong.pet.dto.AdminItemSummary;
import com.nyangmeong.pet.dto.BakeItemSummary;
import com.nyangmeong.pet.dto.ShopItemSummary;

/**
 * [아이템 Mapper]
 */
@Mapper
public interface ItemMapper {

    /**
     * 아이템 이미지 첨부파일 번호 조회
     */
    Long selectAtchFileSn(Long itemSn);

    /**
     * 상점 아이템 목록 조회 (부위 없으면 전체)
     */
    List<ShopItemSummary> selectShopItems(@Param("pixelCharSn") Long pixelCharSn,
            @Param("itemRegnCd") String itemRegnCd);

    /**
     * 구매할 판매 중 아이템 조회
     */
    List<ShopItemSummary> selectShopItemsBySn(@Param("pixelCharSn") Long pixelCharSn,
            @Param("itemSns") List<Long> itemSns);

    /**
     * 굽기 대상 아이템 조회 (판매 중이고 굽기 지시 있음)
     */
    List<BakeItemSummary> selectBakeItems();

    /**
     * 굽기 대상 아이템 여부
     */
    int countBakeItem(Long itemSn);

    /**
     * 보유 아이템 등록 (미착용)
     */
    int insertHeldItems(@Param("pixelCharSn") Long pixelCharSn, @Param("items") List<ShopItemSummary> items);

    /**
     * 관리자 아이템 목록 조회 (판매 중지 포함, 부위 없으면 전체)
     */
    List<AdminItemSummary> selectAdminList(@Param("itemRegnCd") String itemRegnCd);

    /**
     * 아이템 조회
     */
    Item selectItem(Long itemSn);

    /**
     * 부위의 아이템 번호 목록 조회 (판매 중지 포함)
     */
    List<Long> selectItemSnsByRegion(String itemRegnCd);

    /**
     * 부위의 마지막 정렬 순서 조회 (없으면 0)
     */
    long selectMaxSortSeq(String itemRegnCd);

    /**
     * 아이템을 보유한 캐릭터 수
     */
    long countHolders(Long itemSn);

    /**
     * 아이템 등록
     */
    int insert(Item item);

    /**
     * 아이템 수정 (null 칸은 그대로, bakeHintChanged 면 굽기 지시를 bakeHint 로 바꿈)
     */
    int update(@Param("item") Item item, @Param("bakeHintChanged") boolean bakeHintChanged);

    /**
     * 아이템 정렬 순서 수정
     */
    int updateSortSeq(@Param("itemSn") Long itemSn, @Param("sortSeq") long sortSeq,
            @Param("actorId") String actorId);
}
