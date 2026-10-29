# JAJ 핸드오프

## 지금 하는 것

끝남: SCR-HLT-005 검진상세 — 기본정보 상세 조회, 첨부 자료 목록·등록·파일·교체·삭제, PDF 분석(가짜 응답), 혈액검사 수치 목록·선택 삭제, 기타검사 CRUD, 처방약 CRUD(투약 케어 항목 연동·매일 0시 자동 종료), 반복 예방 등록·해지(정기관리 항목 연동) API(FN-HLT-005-01~05·11~15). 검증 1회차 `확인 못 함 있음`(연결 남음만), 사람 확인 완료 (2026-10-29)

## 다음에 할 것

- 연결 남음: FN-HLT-005-01~03·05·11~15 (프론트 연결 검진상세 페이지 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-005 검진상세 · API 연결")
- 연결 남음: FN-HLT-005-04 (PDF 분석 외부 API 결정 issues 6, JAJ). `health/service/InspectionAnalyzer`의 가짜 결과를 실제 호출로 바꾼다. 같은 첨부 동시 요청 409는 테스트가 없다
- 참고: 검진상세 목록 API(첨부·검사 수치·기타검사·처방약·반복 예방)는 `ckupRcdSn` 필수. HLT-011 종합 건강기록의 반려동물 전체 조회는 그 마일스톤에서 추가(SCHDL 조인이 `MapperXmlRuleTest` 조건에 걸리므로 방법을 정한다)
- 참고: [＋ 검사 수치 추가] 인라인 저장 `POST inspection-values`는 SCR-HLT-007에서 만든다. 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 연결 남음: FN-HLT-003-01·12·13·21~24·38 (프론트 연결 케어리스트 탭 드롭다운·목록·기록 내용·완료 토글·★ 토글·관리 항목 추가 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 토탈 케어리스트 API 연결"). 메인페이지 MAN-009는 YAY가 `PUT care-item-exposures`를 쓴다
- 연결 남음: 추가-1 (`searchNamesByPets`를 `GET /internal/ai/register-context`에 연결, LJS). 기본 항목은 케어리스트를 처음 열 때 생기므로 한 번도 안 연 반려동물은 기본 항목명이 빠진다
- 결정 필요: AI로 등록한 케어 항목의 출처 저장(LJS 질문). `CARE_ARTCL`에 출처 컬럼이 없어 지금은 저장하지 않는다. 저장하려면 `db/migration` 추가를 팀과 맞춘다. 일정은 `schdlSrcCd`=HLTC0067로 이미 저장된다
- 참고: `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~9999 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 참고: 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
- 공용 DB 정리: 반려동물 1의 확인용 항목 "검증용 정기"(3272·3273)가 남아 있으면 `DELETE /api/pets/1/care-items/{번호}`로 지운다
- 연결 남음: FN-HLT-002-05·06·14·26~31·46 (프론트 연결 검진기록 탭, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 검진기록 API 연결"). 목록 건수 필드는 `totalElements`(기획의 `totalCount` 아님)
- 연결 남음: FN-HLT-002-31 영수증 [직접 입력] 경로의 영수증 연결·상태 변경, `CheckupRecordFake`를 `CheckupService`로 바꾸기 (LJS와 맞춘다)
- 연결 남음: 추가-1 (프론트 연결 `features/health/api.js` 등 일정 화면, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 일정 API 연결")
- 연결 남음: FN-HLT-003-39 (반려동물 목록 API, SJM)
- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장 화면 SCR-AIM-012, LJS)
- 참고: 기획의 `SCHEDULE_NOT_FOUND`는 기존 `CARE_SCHEDULE_NOT_FOUND`를 그대로 썼다(ErrorCode 추가 없음)
- HLTG21(캘린더 뱃지 색상) 하위 코드가 `db/` 공통코드에 없다. 코드 값을 정해 `db/migration`에 넣는 것을 팀과 맞춘다(그 전엔 색상 저장 불가)
- 통합 캘린더 호출 방식(issues 공통레이아웃·메인 8번): 지금은 반려동물별 호출. 다건 조회 API가 필요하면 다시 정한다
- LJS `CareScheduleAlarmService`(AIM-006)·`CheckupRecordFake`(AIM-009)를 health service로 연결하는 것은 LJS와 맞춘다
- `application.properties`에 빠진 키: `app.front-base-url`, `app.seed.password` (디스코드의 최신 파일로 받는다)
