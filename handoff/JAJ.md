# JAJ 핸드오프

## 지금 하는 것

끝남(커밋 전). 건강기록 문구 통일 DB 데이터 2건(2026-11-13).
- 검진유형 HLTC0005 이름 '기타 진료' → '기타진료': `db/20261109_insert_base_data_v0.14.sql` 변경 절 v0.14 (13) 추가(본문은 그대로, 번호는 DDL 파일과 같이 매겨 (10)~(12)는 DDL이 씀). 시드(RGTR_ID='SEED') `SCHDL.SCHDL_NM` 93행, `CKUP_RCD.CKUP_CN` 문장 앞 '기타진료 진행.' 93행(다음 내원 시 재검 권장 38·특이사항 없음 29·약 처방 후 경과 관찰 26), `docs/api/health.md` ckupTypeCd 설명. 코드는 HLTC0005 코드 값으로만 비교해 고칠 곳 없음
- 처방 투약 케어 항목명 '{약 이름} 복용' → 약 이름: 시드 `CARE_ARTCL` 208행(SN 2943~3271, 전부 PRSC.MDCN_NM과 일치). 서비스 코드는 이미 약 이름 그대로라 안 고침
- 공용 DB 반영 완료(2026-11-13): COM_CD 2, KWRD_DICT 1, SCHDL 93, CARE_ARTCL 208, CKUP_RCD 93 (사람이 실행, CKUP_RCD는 12:38). 실행한 SQL은 "다음에 할 것" 첫 항목

직전: SCR-HLT-007 검사 수치 결과값·참고범위를 0 이상, 정수 7자리·소수 둘째 자리까지로 제한(FN-HLT-007-02·03, ※신규 정책: 검사 수치 0 이상 · 정수 7자리). `InspectionValueCreateRequest`·`InspectionValueUpdateRequest`에 `@DecimalMin("0") @Digits(integer = 7, fraction = 2)`, DB decimal(15,4)는 그대로. PDF 분석 결과도 같은 `POST inspection-values`라 따로 막을 경로 없음. 공용 DB 확인: 까미(1번) 검진기록 5에서 -5·10000000·하한 -1 → 400, 9999999.99 → 201(확인 기록 INSP_NVL 3103 삭제함), PATCH -5 → 400(사람 확인 2026-11-11)

이전: SCR-HLT-002 일상 기록 시각·관찰 촬영일 미래 입력 막기는 #225, 일상 기록 1회 상한은 #222, 시드 수치 보정은 #223으로 병합됨

## 다음에 할 것

- 공용 DB 반영 완료(2026-11-13): COM_CD 2, KWRD_DICT 1, SCHDL 93, CARE_ARTCL 208, CKUP_RCD 93. 아래는 실행한 SQL 기록이다(다시 실행하지 않는다)
  ```sql
  SET NAMES utf8mb4;
  -- 기준 데이터 v0.14 (13)
  UPDATE `COM_CD` SET `COM_CD_NM`='기타진료', `COM_CD_EXPLN`='기타진료 — 일정 종류 ''기타''(HLTG19)와 표기 충돌을 피하기 위한 명칭 (09-15 회의)', `MDFCN_DT`=NOW() WHERE `COM_CD`='HLTC0005';
  UPDATE `COM_CD` SET `COM_CD_EXPLN`='기타진료·그 밖의 일정 기본 톤', `MDFCN_DT`=NOW() WHERE `COM_CD`='HLTC0083';
  UPDATE `KWRD_DICT` SET `INTRP_RSLT_CN`='검진유형 = 기타진료(HLTG01)', `MDFCN_DT`=NOW() WHERE `DICT_USG_CD`='AIMC0037' AND `MPNG_CD`='HLTC0005' AND `INTRP_RSLT_CN`='검진유형 = 기타 진료(HLTG01)';
  -- 시드 일정 이름 (시드 93행)
  UPDATE `SCHDL` SET `SCHDL_NM`='기타진료', `MDFCN_DT`=NOW() WHERE `SCHDL_NM`='기타 진료' AND `CKUP_TYPE_CD`='HLTC0005' AND `RGTR_ID`='SEED';
  -- 시드 검진 내용 문장 앞 (시드 93행)
  UPDATE `CKUP_RCD` SET `CKUP_CN`=CONCAT('기타진료 진행.', SUBSTRING(`CKUP_CN`, CHAR_LENGTH('기타 진료 진행.') + 1)), `MDFCN_DT`=NOW() WHERE `RGTR_ID`='SEED' AND `CKUP_TYPE_CD`='HLTC0005' AND `CKUP_CN` LIKE '기타 진료 진행.%';
  -- 처방 투약 케어 항목명 (시드 208행)
  UPDATE `CARE_ARTCL` A JOIN `PRSC` P ON P.`PRSC_SN` = A.`PRSC_SN` SET A.`CARE_ARTCL_NM` = P.`MDCN_NM`, A.`MDFCN_DT` = NOW() WHERE A.`CARE_ARTCL_NM` = CONCAT(P.`MDCN_NM`, ' 복용') AND A.`RGTR_ID`='SEED';
  ```
- 연결 남음: FN-HLT-002-11·13 일상 기록 시각 (프론트 연결: 오늘 기록 시간 입력이 지금 이후 시각을 고를 수 있으면 막고, 400 `rcdDt` 메시지를 보여 줄지 정하기, 프론트 저장소에서 /start)
- 연결 남음: FN-HLT-003-29·31·40 회원 단위 일정 조회 (프론트 연결 `features/health/api.js`의 `getSchedules` 하나만 `GET /api/care-schedules`로, `CareCalendarPage.jsx`·`UpcomingSchedules.jsx`·`queries.js`는 고치지 않음, 프론트 저장소에서 /start)
- 팀원 전달: `PetAuthorizer.searchCaregiverPetSns`·`PetAuthMapper.selectCaregiverPetSns` 추가(공유 파일, 기존 `checkCaregiver`·`checkPrimary`는 그대로). 디스코드에 전달 문구 올리기
- LJS의 AI 대화 일정 저장 연결(FN-AIM-012-13·14) 후 AI 생성 일정·케어 항목이 케어 캘린더와 케어리스트에 보이고 케어미션에는 반영되지 않는지 확인
- 공용 DB 보호로 호출하지 않은 3가지(새 반려동물 등록 후 기본 11개 생성, 일정의 반려동물 변경, 검진기록의 반려동물 변경)는 로컬 DB에서 확인
- 기준 SQL 반영(코드는 끝, `db/`는 고치지 않음): `db/20261109_create_tables_v0.14.sql` `CARE_ARTCL`의 `MAIN_EXPSR_YN`·`MAIN_SORT_SEQ` 컬럼, `IDX_CARE_ARTCL_1`을 `(PET_SN, DEL_YN)`으로, `CK_CARE_ARTCL_MAIN_EXPSR_YN` 제약, 맨 위 "변경" 절에 기존 DB용 SQL. `db/seed/20261028_seed_data.sql` 91행 `CARE_ARTCL` INSERT 컬럼과 VALUES. 공용 DB는 이미 컬럼이 없으니 남은 인덱스·제약만 확인
- `docs/api/health.md` 41·208·312행의 "★ 해제" 문구 지우기
- (결정 대기) 신규 정책 처리(※신규 정책 표시 / 요구사항 ID 발급): 회의에서 정해지면 `docs/api/health.md` 두 행의 기능 ID 칸과 마일스톤 절을 고친다

## 환경

- `application.properties`에 빠진 키: `app.sns.naver.redirect-uri`·`app.sns.kakao.redirect-uri`·`app.sns.google.redirect-uri` (디스코드의 최신 파일로 받는다, 건강기록 작업에는 안 쓴다)
- 로컬 `application.properties`는 `springdoc.*.enabled=false`라 Swagger를 보려면 `./gradlew bootRun --args='--springdoc.api-docs.enabled=true --springdoc.swagger-ui.enabled=true'`로 띄운다

## 참고

- Git Bash(Windows)에서 `TZ=Asia/Seoul date`는 시간대가 적용되지 않아 UTC가 나온다. 서울 시각은 `date -u -d '+9 hours'`로 만든다
- 회원 단위 일정 조회 비교는 양육 반려동물 11마리인 mbrSn=6으로 했다. 이 회원이 tester@nyangmeong.dev인지는 이메일 암호화로 대조하지 못했다
- `PetAuthorizer.searchCaregiverPetSns`는 역할 코드를 `BrupRole.PRIMARY`·`CO_PARENT`로 고정해 넘긴다. 양육 역할이 늘면 이 목록도 본다
- 공용 DB에 v0.14 (3)·(4)(HLTG25 그룹까지)·(5)를 실행함(2026-11-09). 공통코드 번호를 새로 정할 때는 기준 파일 본문뿐 아니라 `db/archive/` 변경 절과 공용 DB도 본다(HLTC0079 중복 사례)
- 케어 일정 알림 petSn 변경 전에 생긴 알림은 링크가 `/health/calendar` 그대로다
- 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 공용 DB에 남은 확인용 데이터: 회원 1(반려동물 3, 검진일 2026-10-31) 검진 포인트 20P(원장 5634), 영수증 179 상태 이력 1364행, 반려동물 2의 관찰 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행. 반려동물 1의 `PET_BDWG`는 17.513(남은 최신 기록 91). 반려동물 1004(회원 1)의 2026-11-10 미션 "화장실 청소하기" 완료(DAY_MSN 84221, 10P)는 확인 호출로 생겼고 그대로 둔다(사람 결정 2026-11-10). 공용 DB의 HLTG21 코드 5개는 실제 코드라 그대로 둔다
- 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(사람 확인 2026-10-29). 참고범위는 PATCH `refRange`로 덮어쓰거나 비운다(2026-11-02)
- 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~365 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
- curl로 상태 변경 API를 부를 때는 `X-Requested-With: XMLHttpRequest` 헤더가 있어야 한다(없으면 403, `RequestedWithFilter`). 한글 본문은 UTF-8 파일로 `--data-binary @파일`로 보낸다(Git Bash 인자로 넘기면 Invalid UTF 400)
