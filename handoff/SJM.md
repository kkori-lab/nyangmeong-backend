# SJM 핸드오프

## 지금 하는 것
케어미션 출제 배치 개선 (SCR-MYP-020 후속) — 기획 (시작 2026-10-28 01:34)
- 기준: SCR-MYP-020 결정5(출제)·결정7(정산)과 FN-MYP-020-03·05. 규칙은 그대로이고 저장 방식만 바꾼다(명세 변경 없음이라 DMS는 다시 읽지 않음). 마일스톤 SCR-MYP-020은 `끝` 그대로 둔다
- 원인: `issue`가 960마리 × 8개를 한 행씩 INSERT(7,704번 왕복)하고 전체가 한 트랜잭션이라 30분쯤 걸리고 끝날 때까지 안 보인다. `settle`도 같은 모양(행마다 UPDATE 1번)이라 다음 날 자정 정산이 같은 만큼 걸린다. 그래서 정산도 같이 고친다
- 결정1 출제 묶음: `CareMissionJob`이 `CareMissionService.searchIssueTargets(ymd)`(readOnly, 그날 미션이 없는 주양육 대상만 SQL `NOT EXISTS`로)를 한 번 부르고, 100마리씩 나눠 `CareMissionService.issue(ymd, targets, obsrvPetSns)`(@Transactional)를 묶음마다 부른다. 묶음마다 커밋되므로 진행 중에도 앞 묶음은 보인다. 묶음 하나는 여러 행 INSERT 한 문장(`insertDayMissions`, `<foreach>`, 최대 800행)이다. 반려동물 단위가 아니라 100마리 단위인 이유: 마리마다 커밋하면 왕복이 960번 남는다. 100마리면 10번이다
  - 출제 순서(`DAY_MSN_SN` 오름차순)는 여러 행 INSERT의 VALUES 순서대로 자동 증가 값이 붙으므로 유지된다(InnoDB 단순 INSERT)
  - 다시 돌려도 안전: 대상 조회가 이미 출제된 동물을 빼고, 동시에 겹치면 UK_DAY_MSN_1이 막아 그 묶음만 롤백된다(다음 정각에 다시 채움)
  - 기존 `selectIssuedPetSns`·`insertDayMission`(한 행)은 쓰는 곳이 없어지면 지운다
- 결정2 정산 묶음: `settle(ymd)`(@Transactional 하나 그대로)를 (1) 포인트를 줘야 하는 미완료 행(진행률 50% 초과)만 조회해 지금처럼 한 행씩 `updateDayMissionSettled` + `earnMission` (2) 나머지(완료 또는 50% 이하)를 UPDATE 한 문장 `updateDayMissionsSettled`로 `CLCLN_CFMTN_YN='Y'`, `GIVE_PNT = CASE WHEN CMPTN_YN='Y' THEN GIVE_PNT ELSE 0 END`(PET `DEL_YN='N'`, CARE_MSN JOIN으로 50% 조건을 SQL에서) 순서로 바꾼다. 지금 미션은 모두 기준 1회라 (1)은 0행이고 정산은 쿼리 2번이다. 포인트 행은 원장 적립과 같은 트랜잭션이라 지금처럼 한 행씩 둔다
- 기록: 출제·정산 끝에 INFO 로그 한 줄(기준일, 출제 마리 수·정산 행 수)
- 바꿀 파일: `scheduler/CareMissionJob`, `pet/service/CareMissionService`, `pet/mapper/CareMissionMapper` + `mapper/pet/CareMissionMapper.xml`, 테스트 `pet/service/CareMissionServiceTest`·`scheduler/CareMissionJobTest`. API·DTO·DB 변경 없음(`MissionTargetSummary`는 그대로 씀)
- 완료 조건 — 명령: `./gradlew build -q`. `CareMissionJobTest` 정산을 먼저 하고, 대상 250마리면 `issue`를 100·100·50마리로 3번 부르며 관찰 목록을 넘긴다, 대상이 없으면 `issue`를 부르지 않는다. `CareMissionServiceTest` 기존 출제 규칙 검사(8개·고양이 산책 없음·대표·관찰·사용 중지 행·활동기반 2개면 일상케어 6개)가 여러 행 INSERT에 넘긴 목록 기준으로 통과, 한 묶음은 `insertDayMissions` 1번, 정산은 포인트 줄 행만 한 행씩 적립하고 나머지는 `updateDayMissionsSettled` 1번, 두 번 돌려도 한 번만 적립. 화면: `app.care-mission.batch-enabled=true`로 로컬 `bootRun`(공용 DB, 오늘 미션이 없을 때) 정각 뒤 1분 안에 출제 로그가 찍히고 `SELECT COUNT(*) FROM DAY_MSN WHERE MSN_CRTR_YMD = CURDATE()`가 실행 중에도 100마리 단위로 늘어난다. 정산은 다음 날 정각 로그로 확인
- 하지 않는 것: 출제·정산 규칙 변경, 반영(`reflect`) 변경, 인덱스 추가(`UK_DAY_MSN_1`이 PET_SN·날짜로 시작해 `NOT EXISTS`에 쓰임)
- 새 의존성: 없음

## 다음에 할 것
- LJS에게 `MasterSeeder` 미션 목록 변경 알리기(21개로 바뀜, `CharacterSeeder`는 그대로)
- 연결 남음: 프론트 SCR-MYP-020 포인트 카드·요약·내역·케어미션 탭과 SCR-MYP-001 요약(프론트 저장소에서 /start, SCR-MYP-003은 체중 잠금은 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 판단, 기본 선택 동물은 `rprsPetYn`, 털색 드롭다운 설명), JAJ 건강 일지 기록·케어리스트 기본 항목 체크·일정·관찰 기록 → `reflect`, 검진기록·영수증 빠른 등록 → `earnCheckupPoint`, 관찰 중 반려동물 조회(`scheduler/ObservationFake` 대체), KSJ 펫이야기 글·댓글 → `reflectRepresentative`, LJS `PointFake` → `PointService.use`, 가계부 지출 → `reflect`(반려동물 있는 지출만)
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
