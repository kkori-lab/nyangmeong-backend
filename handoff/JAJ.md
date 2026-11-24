# JAJ 핸드오프

## 지금 하는 것

진행 중인 작업 없음. 마지막 작업은 검진기록 ↔ 케어 캘린더 일정 연동 정리(SCR-HLT-002, ※신규 정책, 추가-1·2 포함)로 끝남(사람 확인 2026-11-23). 결정과 결과는 `milestones/JAJ.md` 해당 절

## 다음에 할 것

- 연결 남음: FN-HLT-005-01·002-26~29·003-29~37 검진기록 일정 연동 정리 (프론트 연결 검진상세 수정 잠금·`schdlDelYn` 안내·일정 `ckupRcdSn` 이동을 실제 응답으로 확인, 프론트 저장소에서 /start. 프론트 `milestones/JAJ.md`에 "SCR-HLT-005 검진상세 · 검진기록 일정 연동 정리 연결 — 대기" 절 넣음)
- 연결 남음: FN-HLT-003-12 케어리스트 개편 (프론트 연결 토탈 케어리스트·「케어 항목 관리」 모달·튜토리얼 3단계·검진기록/처방약/반복 예방 삭제 확인창, 프론트 저장소에서 /start. 프론트 `milestones/JAJ.md`에 "SCR-HLT-003 케어 캘린더 · 케어리스트 개편 연결 — 대기" 절 넣음)
- 연결 남음: FN-HLT-003-29·31·40 회원 단위 일정 조회 (프론트 연결 `features/health/api.js`의 `getSchedules` 하나만 `GET /api/care-schedules`로, `CareCalendarPage.jsx`·`UpcomingSchedules.jsx`·`queries.js`는 고치지 않음, 프론트 저장소에서 /start)
- 연결 남음: FN-HLT-002-11·13 일상 기록 시각 (프론트 연결: 오늘 기록 시간 입력이 지금 이후 시각을 고를 수 있으면 막고, 400 `rcdDt` 메시지를 보여 줄지 정하기, 프론트 저장소에서 /start)
- 팀원 전달(디스코드): 공유 파일 `PetAuthorizer.isPrimary`·`PetAuthorizer.searchCaregiverPetSns`·`PetAuthMapper.selectCaregiverPetSns` 추가(기존 `checkCaregiver`·`checkPrimary`는 그대로), pet `CareMissionService.searchByPet`(읽기 전용, SJM 알림 완료), `CARE_ARTCL.INDCT_YN` 컬럼(공용 DB 반영 완료)
- LJS의 AI 대화 일정 저장 연결(FN-AIM-012-13·14) 후 AI 생성 일정·케어 항목이 케어 캘린더와 케어리스트에 보이고 케어미션에는 반영되지 않는지 확인
- 공용 DB 보호로 호출하지 않은 2가지(새 반려동물 등록 후 기본 11개 생성, 일정의 반려동물 변경 — 연결 검진기록의 반려동물도 따라감)는 로컬 DB에서 확인. 검진기록 수정으로는 반려동물·검진일을 더 바꾸지 않는다
- `docs/api/README.md` API 개수(176개·JAJ 50)를 실제(health.md 55개)에 맞추기(사람 결정 2026-11-18, 따로 한다)
- (결정 대기) 신규 정책 처리(※신규 정책 표시 / 요구사항 ID 발급): 회의에서 정해지면 `docs/api/health.md` 해당 행의 기능 ID 칸과 마일스톤 절을 고친다

## 환경

- `application.properties`에 빠진 키: `app.sns.naver.redirect-uri`·`app.sns.kakao.redirect-uri`·`app.sns.google.redirect-uri` (디스코드의 최신 파일로 받는다, 건강기록 작업에는 안 쓴다)
- 로컬 `application.properties`는 `springdoc.*.enabled=false`라 Swagger를 보려면 `./gradlew bootRun --args='--springdoc.api-docs.enabled=true --springdoc.swagger-ui.enabled=true'`로 띄운다

## 참고

- Git Bash(Windows)에서 `TZ=Asia/Seoul date`는 시간대가 적용되지 않아 UTC가 나온다. 서울 시각은 `date -u -d '+9 hours'`로 만든다
- `PetAuthorizer.searchCaregiverPetSns`는 역할 코드를 `BrupRole.PRIMARY`·`CO_PARENT`로 고정해 넘긴다. 양육 역할이 늘면 이 목록도 본다
- 공용 DB에 v0.14 (3)·(4)(HLTG25 그룹까지)·(5)를 실행함(2026-11-09). 공통코드 번호를 새로 정할 때는 기준 파일 본문뿐 아니라 `db/archive/` 변경 절과 공용 DB도 본다(HLTC0079 중복 사례)
- 케어리스트 기본 항목 새 이름 4개(PR #262)에 맞춰 `CARE_MSN` 미션 이름과 시드 118·680줄(pet 도메인)은 바꾸지 않는다. 공용 DB 반영은 v0.14 (20)(2026-11-18, 3,940건)
- 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 공용 DB에 남은 확인용 데이터: 회원 1(반려동물 3, 검진일 2026-10-31) 검진 포인트 20P(원장 5634), 영수증 179 상태 이력 1364행, 반려동물 2의 관찰 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행. 반려동물 1의 `PET_BDWG`는 17.513(남은 최신 기록 91). 반려동물 2(회원 1)의 일정 4994·검진기록 1044·1045(연동 정리 확인용, 삭제 처리됨). 반려동물 1004(회원 1)의 2026-11-10 미션 "화장실 청소하기" 완료(DAY_MSN 84221, 10P)는 확인 호출로 생겼고 그대로 둔다(사람 결정 2026-11-10). 공용 DB의 HLTG21 코드 5개는 실제 코드라 그대로 둔다
- 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 고른 적 없는 기본 항목의 표시여부는 `BasicCareItem` 이름으로 추천 5개를 판단한다. 이름이 다른 시드 기본 항목(아래 줄)은 추천으로 안 잡혀 보임(true)으로 나온다
- `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~365 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
- curl로 상태 변경 API를 부를 때는 `X-Requested-With: XMLHttpRequest` 헤더가 있어야 한다(없으면 403, `RequestedWithFilter`). 한글 본문은 UTF-8 파일로 `--data-binary @파일`로 보낸다(Git Bash 인자로 넘기면 Invalid UTF 400)
