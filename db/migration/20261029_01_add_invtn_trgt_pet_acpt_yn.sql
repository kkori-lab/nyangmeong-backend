-- 초대대상반려동물 수락 여부 컬럼 추가
-- 이유: 초대 하나에 묶인 반려동물 중 일부만 수락할 수 있어 반려동물별 수락·거절을 남길 곳이 필요함
--       (JNT_BRUP_INVTN.INVTN_STTS_CD는 초대 단위)
-- 근거: 마이페이지 기능명세서 FN-MYP-005-03·005-05, 화면설계서 SCR-MYP-005 5-2·5-3
-- 값: NULL 응답 전, Y 수락, N 거절
-- 기존 응답한 초대는 수락 = 받는 회원의 양육관계가 있는 반려동물만 Y, 거절 = 전부 N으로 채움
SET NAMES utf8mb4;

ALTER TABLE `INVTN_TRGT_PET`
  ADD COLUMN `ACPT_YN` char(1) NULL COMMENT '수락여부' AFTER `PET_SN`,
  ADD CONSTRAINT `CK_INVTN_TRGT_PET_ACPT_YN` CHECK (`ACPT_YN` IN ('Y','N'));

UPDATE `INVTN_TRGT_PET` T
  JOIN `JNT_BRUP_INVTN` I
    ON I.`JNT_BRUP_INVTN_SN` = T.`JNT_BRUP_INVTN_SN`
   SET T.`ACPT_YN` = CASE WHEN I.`INVTN_STTS_CD` = 'PETC0021'
                           AND EXISTS ( SELECT 1
                                          FROM `BRUP_REL` R
                                         WHERE R.`PET_SN` = T.`PET_SN`
                                           AND R.`MBR_SN` = I.`RCVR_MBR_SN`
                                      )
                          THEN 'Y'
                          ELSE 'N'
                     END
 WHERE I.`INVTN_STTS_CD` IN ('PETC0021', 'PETC0022');
