-- 게시판 공통코드(CMUG01)에 실종센터 추가
-- 이유: 실종센터 게시글(PST)도 펫이야기와 같은 PST 테이블을 쓰는데 BBS_CD에 넣을 코드가 없음
-- 근거: 커뮤니티 기능명세서 FN-CMU-002-10, 화면설계서 SCR-CMU-002
-- 값: CMUC0024 실종센터 (PST.BBS_CD, 코드 최대값 CMUC0023 다음)
SET NAMES utf8mb4;

INSERT INTO `COM_CD` (`COM_CD_PRNT_SN`,`COM_CD`,`COM_CD_NM`,`COM_CD_EXPLN`,`SORT_SEQ`)
VALUES ((SELECT p.`COM_CD_SN` FROM (SELECT `COM_CD_SN` FROM `COM_CD` WHERE `COM_CD`='CMUG01') p),'CMUC0024','실종센터','LOST_TPOF와 1:1인 게시글',30);
