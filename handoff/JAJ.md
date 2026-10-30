# JAJ 핸드오프

## 지금 하는 것

SCR-HLT-002 건강 일지 (지속 관찰) — 끝남: 검증 `확인 못 함 있음`(연결 남음만), 사람 확인 통과 (승인 2026-10-30 09:27, 시작 2026-10-30 09:12)

공통: 새 파일은 `health/` 아래 `ObservationController`·`ObservationService`·`ObservationMapper`(+`mapper/health/ObservationMapper.xml`)·VO `ObservationItem`·`ObservationCycle`·enum `ObservationStatus`(HLTG18)·DTO(`docs/api/health.md`의 Observation* 그대로 + `ObservationCycleSearchCond`·`ObservationItemSearchCond`). 권한은 `petAuthorizer.checkCaregiver`. 조회는 모두 `DEL_YN='N'`. 회차 번호 `cyclNo`는 저장하지 않고 `ROW_NUMBER() OVER (PARTITION BY OBSRV_ARTCL_SN ORDER BY OBSRV_DT, OBSRV_CYCL_SN)`로 매번 계산한다(삭제 뒤 자동으로 1회차부터 다시 매겨짐). 오류 코드는 이미 있는 `OBSERVATION_ITEM_NOT_FOUND`·`OBSERVATION_ITEM_ENDED`·`OBSERVATION_CYCLE_NOT_FOUND`를 쓴다(ErrorCode 추가 없음). 사진은 `FileStore.store`(비공개, jpg·jpeg·png·webp·10MB·이중 검증은 FileStore가 함).

- FN-HLT-002-15 관찰 항목 목록: `GET /api/pets/{petSn}/observation-items?obsrvSttsCd=` → `List<ObservationItemSummary>`(firstCycle·lastCycle·cyclCnt·lastSzDiff). 항목과 회차를 두 쿼리로 읽고 service에서 묶는다. 정렬은 관찰중 먼저, 같은 상태 안에서 최근 관찰일 내림차순. `lastSzDiff`는 마지막 회차 크기 − 크기를 입력한 그 이전 회차 크기(어느 쪽이든 없으면 null). 완료 조건 명령: `./gradlew build -q`(ObservationServiceTest·ObservationMapperXmlTest). 화면: 종료 항목이 목록 끝에 오고, 1·3회차 크기 3.0·3.4·2회차 크기 없음이면 lastSzDiff=0.4
- FN-HLT-002-16 선택 항목 헤더·[관찰 종료]: `POST /api/pets/{petSn}/observation-items/{obsrvArtclSn}/end` → 상태 HLTC0056·`OBSRV_END_YMD`=오늘·`OBSRV_ARTCL_STTS_HSTRY` 1행, 회차는 그대로. 이미 종료면 아무것도 바꾸지 않고 200. 화면: 종료 뒤 목록 obsrvSttsCd=HLTC0056·obsrvEndYmd 오늘, 회차 수 그대로
- FN-HLT-002-17·18·19 필름스트립·크기 그래프·선택 회차 상세: `GET /api/pets/{petSn}/observation-cycles?obsrvArtclSn=`(필수) → `List<ObservationCycleSummary>` 관찰일 오름차순, `GET …/observation-cycles/{obsrvCyclSn}/photo` → 파일 자체(`CheckupAttachmentController.getFile`과 같은 방식). 그래프·증감은 화면이 계산(서버는 크기 없는 회차도 그대로 준다). 썸네일은 따로 만들지 않고 원본을 준다(API 초안 "원본·썸네일"을 "원본"으로 고친다). 화면: 다른 회원 반려동물의 회차·사진은 403
- FN-HLT-002-19 회차 수정·삭제: `PATCH …/observation-cycles/{obsrvCyclSn}`(multipart, 보낸 값만, 사진 보내면 교체) → `ObservationCycleResponse`. `DELETE …/observation-cycles/{obsrvCyclSn}` → 회차 `DEL_YN='Y'`, 남은 회차가 0이면 항목도 `DEL_YN='Y'`. 종료 항목의 회차 수정·삭제는 허용(명세는 추가만 막음). 화면: 1~4회차 중 2회차 삭제 → 목록 cyclNo 1·2·3, 마지막 1건 삭제 → 항목 목록에서 사라짐
- FN-HLT-002-20~25·32 관찰 기록 등록 모달 A: `POST /api/pets/{petSn}/observation-items`(multipart `ObservationItemCreateRequest`) → 항목(관찰중)+상태 이력+1회차 한 트랜잭션, 201 `ObservationItemResponse`. 부위 HLTG16 검증, 1회차 변화 입력 없음(요청 DTO에 필드 없음). 관찰일 날짜가 오늘 이후면 400. 케어미션 `OBSERVATION_RECORD` 반영. 화면: 미래 관찰일·사진 없음·png 아닌 파일을 png로 바꾼 것 400
- FN-HLT-002-20~25·32 모달 B(회차 추가): `POST /api/pets/{petSn}/observation-cycles`(multipart `ObservationCycleCreateRequest`) → 201 `ObservationCycleResponse`. 종료 항목이면 400 `OBSERVATION_ITEM_ENDED`, 다른 반려동물 항목이면 404. 변화 HLTG17 검증. 케어미션 `OBSERVATION_RECORD` 반영. 화면: 종료 항목에 추가 400, 과거 관찰일로 추가하면 cyclNo가 관찰일 순으로 끼어든다
- 연결(가짜 교체): `scheduler/ObservationFake`(SJM이 둔 가짜, 주석에 JAJ service로 바꾸라고 적힘)를 지우고 `CareMissionJob`이 `ObservationService.searchObservingPetSns()`(관찰중·삭제 제외 항목이 있는 반려동물)를 부르게 한다. scheduler는 4층이라 health 호출 가능. 명령: `ArchitectureTest` 통과
- API 초안 수정(구현 전): `docs/api/health.md` photo 행 "원본·썸네일" → "원본"
- 프론트 연결: FN-HLT-002-15~25·32 건강 일지 지속 관찰 탭·관찰 기록 등록 모달 A/B (프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 지속 관찰 API 연결")

새 의존성: 없음

## 다음에 할 것

- 연결 남음: FN-HLT-002-15~25·32 (프론트 연결 건강 일지 지속 관찰 탭·관찰 기록 등록 모달 A/B, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 지속 관찰 API 연결")
- 참고: 관찰 회차 PATCH는 보낸 값만 바꿔 크기·메모를 비울 수 없다. 확인용 데이터를 지워도 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행은 공용 DB에 남는다(반려동물 2)
- 연결 남음: FN-HLT-002-01·09~13 (프론트 연결 건강 일지 반려동물 드롭다운·건강기록 변화 그래프·오늘의 기록 카드·[오늘 기록추가] 입력 영역·최근 건강기록 표·상세 기록 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 일상 기록 API 연결"). 서버는 날짜별로 묶지 않는다. 그래프는 `size=14` 뒤 가장 이른 날짜를 `fromYmd`로 다시 불러 하루 합계를 맞춘다
- 연결 남음: 프로필 체중 동기화 `health/service/PetWeightFake`를 pet service 메서드로 바꾸기 (SJM. `PetService`에 메서드가 없어 SJM과 맞춘다). 체중 기록 등록·수정·삭제 뒤 남은 최신 체중으로 부른다
- 참고: 확인 중 공용 DB 반려동물 1의 2026-10-29 케어미션(체중·배변)이 반영되었을 수 있다. 확인용 기록은 삭제함
- 연결 남음: FN-HLT-007-01~06 (프론트 연결 검사 수치 입력 페이지 [저장하기]·수정 진입·분석 결과 검수·[다시 분석하기]·[직접 입력하기], 검진상세 [＋ 검사 수치 추가], 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-007 검사 수치 입력 · API 연결")
- 참고: 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(기획의 200에서 바꿈, 사람 확인 2026-10-29). PATCH로는 참고범위를 지울 수 없다(보낸 값만 수정)
- 결정 필요: SDMA가 HLTG12 공통코드에 없어 직접입력 이름으로 저장된다. 코드 추가는 `db/migration`을 팀과 맞춘다
- 연결 남음: FN-HLT-005-01~03·05·11~15 (프론트 연결 검진상세 페이지 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-005 검진상세 · API 연결")
- 연결 남음: FN-HLT-005-04 (PDF 분석 외부 API 결정 issues 6, JAJ). `health/service/InspectionAnalyzer`의 가짜 결과를 실제 호출로 바꾼다. 같은 첨부 동시 요청 409는 테스트가 없다
- 참고: 검진상세 목록 API(첨부·검사 수치·기타검사·처방약·반복 예방)는 `ckupRcdSn` 필수. HLT-011 종합 건강기록의 반려동물 전체 조회는 그 마일스톤에서 추가(SCHDL 조인이 `MapperXmlRuleTest` 조건에 걸리므로 방법을 정한다)
- 참고: 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
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
