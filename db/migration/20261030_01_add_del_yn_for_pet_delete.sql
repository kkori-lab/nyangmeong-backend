-- 반려동물에 딸린 테이블 중 삭제여부(DEL_YN)가 없던 5개에 컬럼 추가
-- 이유: 반려동물을 삭제하면 가계부(EXPND)만 남기고 나머지 관련 기록은 같은 트랜잭션에서 soft delete 한다
--       읽는 쪽에서 PET.DEL_YN만으로 거르면 조건이 빠진 배치가 하나라도 있을 때 삭제된 반려동물의 기록이 다시 쓰인다
-- 근거: 마이페이지 기능명세서 FN-MYP-002-07(가계부 지출 기록은 유지), docs/api/issues.md 마이페이지 3번 2026-10-30 결정
-- 대상: CKUP_RCD(검진기록, health), DAY_MSN(일별미션), CHAR_CRT_JOB(캐릭터생성작업), CHAR_PHOTO(캐릭터사진), INVTN_TRGT_PET(초대대상반려동물)
-- 이미 삭제된 반려동물의 행은 Y로 채운다
SET NAMES utf8mb4;

ALTER TABLE `CKUP_RCD`
  ADD COLUMN `DEL_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER `CKUP_MEMO_CN`,
  ADD CONSTRAINT `CK_CKUP_RCD_DEL_YN` CHECK (`DEL_YN` IN ('Y','N'));

ALTER TABLE `DAY_MSN`
  ADD COLUMN `DEL_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER `CLCLN_CFMTN_YN`,
  ADD CONSTRAINT `CK_DAY_MSN_DEL_YN` CHECK (`DEL_YN` IN ('Y','N'));

ALTER TABLE `CHAR_CRT_JOB`
  ADD COLUMN `DEL_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER `IDMPT_DMND_ID`,
  ADD CONSTRAINT `CK_CHAR_CRT_JOB_DEL_YN` CHECK (`DEL_YN` IN ('Y','N'));

ALTER TABLE `CHAR_PHOTO`
  ADD COLUMN `DEL_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER `NOW_INDCT_YN`,
  ADD CONSTRAINT `CK_CHAR_PHOTO_DEL_YN` CHECK (`DEL_YN` IN ('Y','N'));

ALTER TABLE `INVTN_TRGT_PET`
  ADD COLUMN `DEL_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER `ACPT_YN`,
  ADD CONSTRAINT `CK_INVTN_TRGT_PET_DEL_YN` CHECK (`DEL_YN` IN ('Y','N'));

UPDATE `CKUP_RCD` C
  JOIN `SCHDL` S
    ON S.`SCHDL_SN` = C.`SCHDL_SN`
  JOIN `PET` P
    ON P.`PET_SN` = S.`PET_SN`
   SET C.`DEL_YN` = 'Y'
 WHERE P.`DEL_YN` = 'Y';

UPDATE `DAY_MSN` D
  JOIN `PET` P
    ON P.`PET_SN` = D.`PET_SN`
   SET D.`DEL_YN` = 'Y'
 WHERE P.`DEL_YN` = 'Y';

UPDATE `CHAR_CRT_JOB` J
  JOIN `PET` P
    ON P.`PET_SN` = J.`PET_SN`
   SET J.`DEL_YN` = 'Y'
 WHERE P.`DEL_YN` = 'Y';

UPDATE `CHAR_PHOTO` H
  JOIN `PIXEL_CHAR` X
    ON X.`PIXEL_CHAR_SN` = H.`PIXEL_CHAR_SN`
   SET H.`DEL_YN` = 'Y'
 WHERE X.`DEL_YN` = 'Y';

UPDATE `INVTN_TRGT_PET` T
  JOIN `PET` P
    ON P.`PET_SN` = T.`PET_SN`
   SET T.`DEL_YN` = 'Y'
 WHERE P.`DEL_YN` = 'Y';
