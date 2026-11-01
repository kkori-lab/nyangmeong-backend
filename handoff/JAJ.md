# JAJ 핸드오프

## 지금 하는 것

SCR-HLT-003 케어 캘린더 · 투약 기록 규칙 — 끝남: 검증 `통과`(1회차), 사람 확인 통과 (승인 2026-10-31 02:35, 시작 2026-10-31 02:27)
공통: 서버는 투약 항목의 완료 기록을 하루 1행(`CARE_CMPTN_HSTRY`, 항목+날짜 UK)으로 두고 체크마다 `CMPTN_CYCL`을 1 올린다. 기록 내용 조회의 "N회를 채운 날만"은 develop에 이미 있다(`selectCompletionList`의 `H.CMPTN_CYCL >= COALESCE(P.DAY_MDCN_NMTM, 1)`, Mapper XML 테스트 있음). 남은 것은 취소 두 가지를 API로 나누는 것이다. 지금 `DELETE care-completions/{sn}`은 투약이면 1회만 내려서, 기록 내용에서 취소하면 N-1회가 남아 기록 내용에서만 사라지고 `todayCmptnCnt`는 0이 되지 않는다. 체크 저장 방식·`todayCmptnCnt`(오늘 행의 `CMPTN_CYCL` 합)는 바꾸지 않는다. 권한은 `petAuthorizer.checkCaregiver`, 잠금은 지금 `cancel`과 같이 항목 → 완료 기록 순서, 취소 뒤 `refreshDates`. 케어미션 반영은 되돌리지 않는다(지금과 같다).
- FN-HLT-003-12 기록 내용 조회(N회 채운 날만): 서버 변경 없음. 서비스 테스트만 더한다. 바꿀 파일: `CareItemServiceTest`. 완료 조건 명령: `./gradlew build -q`. 화면: 하루 2회 투약 항목을 1회 체크하면 `GET …/care-completions`에 없고, 2회 체크하면 그날 1건(`cmptnCycl`=2)
- FN-HLT-003-12 기록 내용 아이콘 → 그날 완료 기록 취소(일괄): 기존 `DELETE /pets/{petSn}/care-completions/{careCmptnHstrySn}`를 "그날 기록 전부 취소"로 바꾼다. 투약도 회차와 관계없이 그 행을 `DEL_YN='Y'`(`deleteCompletion`). 새 경로를 만들지 않는 이유: 기록 내용 행의 `careCmptnHstrySn`이 곧 그날 1행이라 경로가 그대로 맞고, 프론트 연결 전이라 바꿔도 깨지는 화면이 없다. 바꿀 파일: `CareItemService.cancel`·`CareItemController`(Operation 요약)·`CareItemServiceTest`·`CareItemControllerTest`. 완료 조건 명령: `./gradlew build -q`(투약 `CMPTN_CYCL`=2 기록 취소 → `deleteCompletion` 호출, `updateCompletionCycle` 미호출). 화면: 2회 체크 뒤 기록 내용 `careCmptnHstrySn`으로 DELETE 1번 → 기록 내용에서 사라지고 `GET …/care-items`의 `todayCmptnCnt`=0, 다른 반려동물 기록 번호면 404
- FN-HLT-003-12 케어리스트 투약 [마지막 1회 취소]: 새 API `DELETE /pets/{petSn}/care-items/{careArtclSn}/today-completion/last`. 목록 응답에 기록 번호를 더하지 않고 항목 번호로 오늘 행을 찾는다(목록 행에 이미 `careArtclSn`이 있어 화면이 다른 값을 들고 있을 필요가 없다). 오늘 행이 `CMPTN_CYCL` > 1이면 1 내리고, 1이면 `DEL_YN='Y'`. 투약 항목이 아니면 400(`INVALID_INPUT_VALUE`), 오늘 기록이 없으면 404(`CARE_COMPLETION_NOT_FOUND`), 다른 반려동물 항목이면 404(`CARE_ITEM_NOT_FOUND`). 응답은 `ApiResponse<Void>`(화면은 목록을 다시 부른다). 오늘 행 찾기는 기존 `selectCompletionByDateForUpdate`(삭제된 행도 오므로 `delYn` 확인). 바꿀 파일: `CareItemController`·`CareItemService`(`cancelLastToday` 새 메서드)·`CareItemServiceTest`·`CareItemControllerTest`. ErrorCode 추가 없음. 완료 조건 명령: `./gradlew build -q`. 화면: 2회 체크 뒤 1번 호출 → `todayCmptnCnt`=1·기록 내용에서 사라짐, 한 번 더 → 0, 한 번 더 → 404, 일반 항목 → 400
- API 초안 수정(구현 전): `docs/api/health.md`의 `DELETE care-completions/{careCmptnHstrySn}` 설명을 "그날 완료 기록 취소(투약은 그날 체크 전부)"로, 새 행 `DELETE /pets/{petSn}/care-items/{careArtclSn}/today-completion/last`(투약 오늘 마지막 1회 취소, HLT-003-12, JAJ), `CareCompletionCreateRequest` 설명의 "1회 체크마다 1건"을 "하루 1건에 회차를 올림"으로, `CareCompletionSummary.cmptnCycl` 설명에 "투약은 N회를 채운 날만 나온다"
- 프론트 연결: FN-HLT-003-12 기록 내용 아이콘 취소·[마지막 1회 취소] (프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 토탈 케어리스트 API 연결"에 합친다)
새 의존성: 없음

## 다음에 할 것

- 연결 남음: FN-HLT-003-12 (프론트 연결 케어리스트 기록 내용 아이콘 취소·투약 [마지막 1회 취소], 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 투약 취소 API 연결"). 기록 내용 아이콘은 `DELETE /pets/{petSn}/care-completions/{careCmptnHstrySn}`(투약도 그날 전부), [마지막 1회 취소]는 `DELETE /pets/{petSn}/care-items/{careArtclSn}/today-completion/last`(투약 아니면 400, 오늘 기록 없으면 404). 기록 내용 조회는 투약을 N회 채운 날만 준다
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
