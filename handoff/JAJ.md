# JAJ 핸드오프

## 지금 하는 것

SCR-HLT-011 종합 건강기록 — 끝남: 검증 `확인 못 함 있음`(연결 남음만), 사람 확인 통과 (승인 2026-10-30 12:58, 시작 2026-10-30 12:54). 이후 추가 요청 1~9 — `./gradlew build -q` 통과, 사람 확인 통과(2026-10-30)

공통: 이 화면은 조회 전용이고 새 API를 만들지 않는다. 이미 있는 목록 API 4개(첨부·검사 수치·처방약·반복 예방)의 `ckupRcdSn`을 선택값으로 바꿔, 생략하면 반려동물 전체를 준다. 전체 조회는 `CheckupService.searchCompletedCheckups(petSn)`(새 메서드, 기존 `CheckupMapper.selectList`를 조건 없이 써서 완료 상태 검진기록 전부, 연결 일정이 삭제된 것 포함 — 건강 일지 검진기록 목록과 같은 범위)로 검진기록 번호를 얻은 뒤 각 Mapper에서 `CKUP_RCD_SN IN (...)`으로 읽는다. 그래서 다른 Mapper에 SCHDL 조인이 생기지 않고 `MapperXmlRuleTest` 예외 목록도 늘리지 않는다. 검진기록이 0건이면 쿼리 없이 빈 목록. 권한은 `petAuthorizer.checkCaregiver`(검진기록 1건 조회 때와 같이 403). 조회는 모두 `DEL_YN='N'`. SearchCond 4개의 `@NotNull` 제거·주석 갱신. ErrorCode 추가 없음.
- FN-HLT-011-01 반려동물 선택 탭: 서버 작업 없음. `GET /api/pets`(SJM, 있음)로 칩을 그린다. 프론트 연결: 종합 건강기록 반려동물 탭 (프론트 저장소에서 /start)
- FN-HLT-011-02 요약 바: 서버 작업 없음(조합은 화면). 프로필·알레르기·기저질환 `GET /api/pets/{petSn}`(SJM, 있음 — `petBdwg`·`algyCn`·`dssCn`), 최신 체중 `GET …/daily-health-records?dailyRcdIdctCd=<체중>&size=1`(없으면 `petBdwg`), 최근 검진 `GET …/checkups?size=1`, 검사 수치 아래 011-06 결과의 최근 검진 첫 항목, 현재 처방 아래 011-08 `prscSttsCd=<복용중>` 건수·첫 행. 화면: 체중 일상기록이 있으면 그 최신값, 지우면 프로필 체중. 프론트 연결: 종합 건강기록 요약 바 (프론트 저장소에서 /start)
- FN-HLT-011-03 건강 이력 타임라인: `GET /api/pets/{petSn}/checkups?size=5`(있음) + `GET /api/pets/{petSn}/repeat-preventions`(`ckupRcdSn` 생략 → 반려동물 전체, 시작일 내림차순). `RepeatPreventionSummary`에 `hsptlNm`(출처 검진기록 병원명)을 추가해 service가 검진기록 목록에서 채운다(최신 5건 밖의 검진기록이 출처여도 병원명이 나오게). 시작일이 오늘 이후인 항목 제외·5건 병합·같은 날짜면 검진기록 먼저는 화면이 한다. 바꿀 파일: `RepeatPreventionSearchCond`·`RepeatPreventionSummary`·`RepeatPreventionService`·`RepeatPreventionMapper`(+xml)·`CheckupService`. 완료 조건 명령: `./gradlew build -q`(RepeatPreventionServiceTest·CheckupServiceTest·새 Mapper XML 테스트). 화면: 검진기록 2건에 반복 예방 1건씩이면 `ckupRcdSn` 없이 2건·각 `hsptlNm`이 출처 병원명, 다른 회원 반려동물 403
- FN-HLT-011-04 예방 · 정기검진 이력: 서버 작업 없음. 완료 이력 `GET …/checkups?ckupTypeCd=<예방접종|정기검진>`, 다음 예정일 `GET …/care-schedules?fromYmd=<오늘>&schdlKndCd=<병원>&ckupTypeCd=<같은 유형>&size=1`(둘 다 있음). 화면: 오늘 이후 가장 가까운 같은 유형 병원 일정 1건, 없으면 빈 목록. 프론트 연결: 예방 · 정기검진 이력 (프론트 저장소에서 /start)
- FN-HLT-011-05 알레르기 · 기저질환: 서버 작업 없음. `GET /api/pets/{petSn}`의 `algyCn`·`dssCn`(SJM, 있음). 프론트 연결: 알레르기 · 기저질환 카드 (프론트 저장소에서 /start)
- FN-HLT-011-06·07 검사 결과·검사 수치 추이: `GET /api/pets/{petSn}/inspection-values`(`ckupRcdSn` 생략 → 반려동물 전체). 정렬은 검진일 오름차순 → 검진기록 번호 → 정렬순서 → 수치 번호. `schdlDt`는 service가 검진기록 목록의 `ckupYmd`로 채우고 정렬도 service에서 한다(검진일이 다른 테이블). `inspArtclCd`를 주면 그 항목만. 최근 1회차 고르기·기본 항목(2건 이상인 첫 항목)·직접입력 항목 묶기는 화면. 바꿀 파일: `InspectionValueSearchCond`·`InspectionValueService`·`InspectionValueMapper`(+xml). 완료 조건 명령: `./gradlew build -q`(InspectionValueServiceTest·Mapper XML 테스트). 화면: 검진 3건(날짜 섞어 등록)의 CREA가 검진일 오름차순으로, 각 행 `schdlDt`가 그 검진일
- FN-HLT-011-08 처방 및 투약 이력: `GET /api/pets/{petSn}/prescriptions`(`ckupRcdSn` 생략 → 반려동물 전체, 기존 정렬 복용중 우선·시작일 내림차순 그대로). 바꿀 파일: `PrescriptionSearchCond`·`PrescriptionService`·`PrescriptionMapper`(+xml). 완료 조건 명령: `./gradlew build -q`(PrescriptionServiceTest·Mapper XML 테스트). 화면: 두 검진기록의 처방이 한 목록에 복용중 먼저, `prscSttsCd=<복용중>`이면 복용중만
- FN-HLT-011-09 첨부 자료: `GET /api/pets/{petSn}/checkup-attachments?size=4`(`ckupRcdSn` 생략 → 반려동물 전체, 기존 최신순·페이지 그대로). 파일은 기존 `GET …/checkup-attachments/{ckupRcdAtchSn}`. 바꿀 파일: `CheckupAttachmentSearchCond`·`CheckupAttachmentService`·`CheckupAttachmentMapper`(+xml). 완료 조건 명령: `./gradlew build -q`(CheckupAttachmentServiceTest·Mapper XML 테스트). 화면: 두 검진기록의 첨부가 등록 최신순 4건, `totalElements`는 전체 수
- FN-HLT-011-10 관찰 기록: 서버 작업 없음. `GET …/observation-items?obsrvSttsCd=<관찰중>`(있음, firstCycle·lastCycle 포함), 사진 `GET …/observation-cycles/{obsrvCyclSn}/photo`(있음). 프론트 연결: 관찰 기록 블록 (프론트 저장소에서 /start)
- API 초안 수정(구현 전): `docs/api/health.md` 4행(checkup-attachments·inspection-values·prescriptions·repeat-preventions)의 "`ckupRcdSn` 필수"를 "`ckupRcdSn` 생략 시 반려동물 전체(완료 검진기록, 연결 일정 삭제분 포함)"로, repeat-preventions 행에 HLT-011-03 추가, `RepeatPreventionSummary` 절에 `hsptlNm` 추가
- 프론트 연결: FN-HLT-011-01~10 종합 건강기록 화면 전체 (프론트 저장소에서 /start — 마일스톤 "SCR-HLT-011 종합 건강기록 · API 연결")

새 의존성: 없음

### 추가 요청

| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `CheckupAttachmentController`·`CheckupAttachmentService`·`InspectionAnalyzer`(삭제)·`AnalysisResult`(삭제)·`InspectionAnalysisResponse`(삭제)·`InspectionValueMapper`(+xml `selectArticles` 삭제)·`ErrorCode`(분석 2개 삭제)·`CheckupAttachmentServiceTest`·`HealthOverviewServiceTest`·`docs/api/health.md`·`docs/api/issues.md` 6번 | 명령: `./gradlew build -q`. 화면: `POST …/checkup-attachments/{ckupRcdAtchSn}/analyses` 없음(404), `POST …/inspection-values`는 그대로 | 요청 2026-10-30 13:55 "PDF 분석은 React가 Python `POST /ai/checkups/lab-results`로 직접 보내기로 확정, Java 분석 API 삭제" |
| 추가-2 | `scheduler/CareScheduleAlarmJob`·`scheduler/HospitalScheduleFake`(삭제)·health 병원 일정 조회 Service·Mapper(+xml)·테스트 | 명령: `./gradlew build -q`(PET.DEL_YN='Y' 반려동물 일정이 빠지는 테스트) | 요청 2026-10-30 13:55 "진료 일정 알림 배치의 가짜를 health Service로 교체, SCHDL·PET 모두 DEL_YN='N'" |
| 추가-3 | 없음(조사만) | [파일 / 메서드 / 테이블] 표 보고 | 요청 2026-10-30 13:55 "petSn 없이 여러 반려동물 health 테이블을 읽는 곳 중 P.DEL_YN 조건이 빠진 곳 조사" |
| 추가-4 | 없음(SQL 문만 전달, DB·SQL 파일은 사람이 직접). HLTG12 하드코딩 코드가 있으면 그 파일 | 사람이 SQL 실행. "SDMA = (코드값)" 한 줄 | 요청 2026-10-30 13:55 "HLTG12에 SDMA 공통코드 추가, SQL과 설명만 보내고 손대지 말 것" |
| 추가-5 | `CareItemService`(`searchNamesByPets`에서 기본 케어 항목 생성 재사용)·테스트 | 명령: `./gradlew build -q`(연 적 없는 반려동물 → 기본 11개 이름 포함, 두 번 호출해도 11개) | 요청 2026-10-30 13:55 "이름 조회 시 기본 케어 항목이 없으면 기존 생성 로직으로 생성, 중복 금지" |
| 추가-6 | 없음(확인만) | [항목 / 일치 여부 / 근거 코드] 표 보고 | 요청 2026-10-30 13:55 "AIM 담당자에게 안내한 care-schedules·care-items·searchNamesByPets 계약 검증" |
| 추가-7 | `health/service/HealthPetDeleteListener`(새)·health Mapper 10개(+xml `deleteByPetSn`)·테스트 | 명령: `./gradlew build -q`(리스너가 Mapper 10개 삭제를 petSn·SYSTEM으로 호출, 하나 실패 시 예외 전파, BEFORE_COMMIT, Mapper XML 경로·DEL_YN 조건). 화면: 반려동물 삭제 후 그 반려동물의 health 12개 테이블 행이 DEL_YN='Y' | 요청 2026-10-30 "PetDeletedEvent를 BEFORE_COMMIT으로 받아 health 기록 DEL_YN='Y', 예외 삼키지 말 것" |
| 추가-8 | `CheckupMapper.xml`(selectList·selectDetail·existsLinkedCheckup)·`CareScheduleMapper.xml`(selectList 검진 건수)·`CheckupRecord`(delYn)·`src/test/resources/del-yn-tables.txt`(CKUP_RCD)·Mapper XML 테스트 | 명령: `./gradlew build -q`(CKUP_RCD DEL_YN 조건 테스트) | 요청 2026-10-30 "CKUP_RCD.DEL_YN을 health 조회에 반영" |
| 추가-9 | 없음(정리 SQL 전달, 실행은 사람). `selectExpired`는 바꾸지 않음 | 사람이 건수 SELECT·정리 UPDATE 실행 | 요청 2026-10-30 "이미 삭제된 반려동물의 health 데이터를 정리 SQL로 DEL_YN='Y'(1안), 건수 SELECT 함께" |

## 다음에 할 것

- 알림: AIM 담당(LJS)에게 `searchNamesByPets`가 이제 기본 항목을 만들고 새 트랜잭션(REQUIRES_NEW)으로 돈다고 전한다
- 참고: 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 연결 남음: FN-HLT-011-01~10 (프론트 연결 종합 건강기록 화면 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-011 종합 건강기록 · API 연결"). 반려동물 전체 조회는 첨부·검사 수치·처방약·반복 예방 목록에서 `ckupRcdSn`을 빼고 부른다. 반복 예방 전체 조회에만 `hsptlNm`이 채워진다
- 연결 남음: FN-HLT-002-15~25·32 (프론트 연결 건강 일지 지속 관찰 탭·관찰 기록 등록 모달 A/B, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 지속 관찰 API 연결")
- 참고: 관찰 회차 PATCH는 보낸 값만 바꿔 크기·메모를 비울 수 없다. 확인용 데이터를 지워도 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행은 공용 DB에 남는다(반려동물 2)
- 연결 남음: FN-HLT-002-01·09~13 (프론트 연결 건강 일지 반려동물 드롭다운·건강기록 변화 그래프·오늘의 기록 카드·[오늘 기록추가] 입력 영역·최근 건강기록 표·상세 기록 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 일상 기록 API 연결"). 서버는 날짜별로 묶지 않는다. 그래프는 `size=14` 뒤 가장 이른 날짜를 `fromYmd`로 다시 불러 하루 합계를 맞춘다
- 연결 남음: 프로필 체중 동기화 `health/service/PetWeightFake`를 pet service 메서드로 바꾸기 (SJM. `PetService`에 메서드가 없어 SJM과 맞춘다). 체중 기록 등록·수정·삭제 뒤 남은 최신 체중으로 부른다
- 연결 남음: FN-HLT-007-01~06 (프론트 연결 검사 수치 입력 페이지 [저장하기]·수정 진입·분석 결과 검수(분석은 화면이 Python `POST /ai/checkups/lab-results` 직접)·[다시 분석하기]·[직접 입력하기], 검진상세 [＋ 검사 수치 추가], 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-007 검사 수치 입력 · API 연결")
- 참고: 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(기획의 200에서 바꿈, 사람 확인 2026-10-29). PATCH로는 참고범위를 지울 수 없다(보낸 값만 수정)
- 연결 남음: FN-HLT-005-01~03·05·11~15 (프론트 연결 검진상세 페이지 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-005 검진상세 · API 연결")
- 참고: 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 연결 남음: FN-HLT-003-01·12·13·21~24·38 (프론트 연결 케어리스트 탭 드롭다운·목록·기록 내용·완료 토글·★ 토글·관리 항목 추가 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 토탈 케어리스트 API 연결"). 메인페이지 MAN-009는 YAY가 `PUT care-item-exposures`를 쓴다
- 결정 필요: AI로 등록한 케어 항목의 출처 저장(LJS 질문). `CARE_ARTCL`에 출처 컬럼이 없어 지금은 저장하지 않는다. 저장하려면 `db/migration` 추가를 팀과 맞춘다. 일정은 `schdlSrcCd`=HLTC0067로 이미 저장된다
- 참고: `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~9999 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 참고: 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
- 연결 남음: FN-HLT-002-05·06·14·26~31·46 (프론트 연결 검진기록 탭, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 검진기록 API 연결"). 목록 건수 필드는 `totalElements`(기획의 `totalCount` 아님)
- 연결 남음: FN-HLT-002-31 영수증 [직접 입력] 경로의 영수증 연결·상태 변경, `CheckupRecordFake`를 `CheckupService`로 바꾸기 (LJS와 맞춘다)
- 연결 남음: 추가-1 (프론트 연결 `features/health/api.js` 등 일정 화면, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 일정 API 연결")
- 연결 남음: FN-HLT-003-39 (반려동물 목록 API, SJM)
- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장 화면 SCR-AIM-012, LJS)
- HLTG21(캘린더 뱃지 색상) 하위 코드가 `db/` 공통코드에 없다. 코드 값을 정해 `db/migration`에 넣는 것을 팀과 맞춘다(그 전엔 색상 저장 불가)
- LJS `CheckupRecordFake`(AIM-009)를 health service로 연결하는 것은 LJS와 맞춘다. 병원 일정 알림(AIM-006)은 `CareScheduleService.searchHospitalSchedules`로 연결함
- `application.properties`에 빠진 키: `app.front-base-url`, `app.seed.password` (디스코드의 최신 파일로 받는다)
