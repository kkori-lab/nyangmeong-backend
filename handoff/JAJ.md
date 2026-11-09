# JAJ 핸드오프

## 지금 하는 것

SCR-HLT-011 종합 건강기록 · 건강기록 대시보드 고른 항목 저장 · 기록 있는 항목 빼기 — 끝남: 검증 1회차 통과, 사람 확인 통과. A안(2026-11-09)으로 GET 응답을 `savedYn`·`widgets`만으로 줄임(HealthDashboardMapper·HealthRecordedItemSummary·CheckupType 삭제, MapperXmlRuleTest 예외 2줄 삭제, health.md 설명 삭제). HLTG25·`db/` v0.14 (2)·(3)·PUT 규칙·메인 위젯 삭제는 그대로 (승인 2026-11-09 11:22, 시작 2026-11-09 11:19)

## 다음에 할 것

- 공용 DB: PR 병합 직후 `db/20261109_create_tables_v0.14.sql` 변경 절 v0.14 (2) → `db/20261109_insert_base_data_v0.14.sql` v0.14 (3) 실행(맨 앞 `SET NAMES utf8mb4;`), PR에 완료 댓글. 되돌릴 가능성이 있으면 `MBR_DSHBD_WDGT`의 MANC 행을 먼저 백업
- 연결 남음: 새 기능(ID 없음, ※신규 정책) 건강기록 대시보드 고른 항목 저장 (프론트 연결 `features/health/dashboard/widgetStore.js`·`queries.js` `useHealthWidgets`·`useSaveHealthWidgets`를 `GET·PUT /members/me/dashboard-widgets`로, 항목 키 → HLTG25 코드, `dashboardApi.js` `getRecordedItems`는 대체 없이 삭제(기본 선택은 추천 3개), `widgetItems.js:138` `vacc` `nm` → 「예방접종·정기검진」, 프론트 저장소에서 /start)
- 신규 정책 처리(※신규 정책 표시 / 요구사항 ID 발급)는 회의 결정. 정해지면 `docs/api/health.md` 두 행의 기능 ID 칸과 마일스톤 절을 고친다
- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장, 프론트 `src/features/ai/api.js`의 가짜 `registerToHealth` 교체, LJS가 프론트 저장소에서). 백엔드는 새 API 없이 기존 API를 쓴다. 일정 카드는 `POST /pets/{petSn}/care-schedules`(`schdlSrcCd`=`HLTC0067`, `schdlYmd`+`schdlTm`→`schdlDt`, `schdlTm` 없으면 `hrInptYn=false`, 201 `CareScheduleResponse`), 케어 항목 카드는 `POST /pets/{petSn}/care-items`(`cycleDays`→`cycleDrctInptDayCnt`, 일상관리면 주기·일수·`lastCmptnYmd`를 보내지 않음, `lastCmptnYmd`를 보내면 그날 완료 기록 1건이 생기고 미래 날짜는 400 `CARE_COMPLETION_FUTURE_DATE`). AI 출처 일정은 케어미션에 반영하지 않는다(직접 등록만). 서버 재검증 400은 카드 오류로 보여 준다
- `application.properties`에 빠진 키: `app.front-base-url` (디스코드의 최신 파일로 받는다, 다른 작업을 기다리는 중)
- 참고: 사람 확인용 일정 4929(회원 1, 반려동물 1, 색 HLTC0082)는 `DEL_YN='Y'`로 지웠다. 미션·포인트 변화는 없었다. 공용 DB에 실행한 변경 절 v0.12 (6)의 HLTG21 코드 5개는 실제 코드라 그대로 둔다
- 참고: 사람 확인용 체중 기록 55429(반려동물 1)는 `DEL_YN='Y'`로 지웠다. 반려동물 1의 `PET_BDWG`는 동기화로 17.513(남은 최신 기록 91)이 되었고 그대로 두었다. 미션·포인트 변화는 없었다
- 참고: 사람 확인용 식사 기록 55426~55428(반려동물 1)은 `DEL_YN='Y'`로 지웠다. 그날 미션 변화는 없었다
- 참고: 사람 확인용 일정 4926~4928(회원 1, 반려동물 1·2)은 `DEL_YN='Y'`로 지웠다. 그날 두 반려동물에 해당 미션이 없어 미션·포인트 변화는 없었다
- 참고: 검진 포인트 확인 중 회원 1(반려동물 3, 검진일 2026-10-31)에 20P(원장 5634)가 공용 DB에 남는다. 검진기록 1011~1013은 지웠다
- 참고: 이번 확인 중 영수증 179(회원 48)를 원래 상태(AIMC0002·샘플 멱등 ID)로 되돌렸고, 상태 이력 1364행은 공용 DB에 남는다
- 참고: 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 참고: 관찰 확인용 데이터를 지워도 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행은 공용 DB에 남는다(반려동물 2)
- 참고: 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(기획의 200에서 바꿈, 사람 확인 2026-10-29). 참고범위는 PATCH `refRange`로 덮어쓰거나 비운다(2026-11-02)
- 참고: 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 참고: `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~9999 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 참고: 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
