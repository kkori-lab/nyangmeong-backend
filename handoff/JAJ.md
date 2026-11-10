# JAJ 핸드오프

## 지금 하는 것

없음. 건강기록 백엔드 마일스톤은 모두 `끝`이고, 프론트 연결도 FN-AIM-012-13·14 하나만 남았다(2026-11-09 정리). 마지막 작업은 케어 일정 알림 링크에 `?petSn=`을 붙인 것(`71e64e3`)이다

## 다음에 할 것

- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장, 프론트 `src/features/ai/api.js`의 가짜 `registerToHealth` 교체, LJS가 프론트 저장소에서). 백엔드는 새 API 없이 기존 API를 쓴다. 일정 카드는 `POST /pets/{petSn}/care-schedules`(`schdlSrcCd`=`HLTC0067`, `schdlYmd`+`schdlTm`→`schdlDt`, `schdlTm` 없으면 `hrInptYn=false`, 201 `CareScheduleResponse`), 케어 항목 카드는 `POST /pets/{petSn}/care-items`(`cycleDays`→`cycleDrctInptDayCnt`, 일상관리면 주기·일수·`lastCmptnYmd`를 보내지 않음, `lastCmptnYmd`를 보내면 그날 완료 기록 1건이 생기고 미래 날짜는 400 `CARE_COMPLETION_FUTURE_DATE`). AI 출처 일정은 케어미션에 반영하지 않는다(직접 등록만). 서버 재검증 400은 카드 오류로 보여 준다
- 신규 정책 처리(※신규 정책 표시 / 요구사항 ID 발급)는 회의 결정. 정해지면 `docs/api/health.md` 두 행의 기능 ID 칸과 마일스톤 절을 고친다
- `application.properties`에 빠진 키: `app.sns.naver.redirect-uri`·`app.sns.kakao.redirect-uri`·`app.sns.google.redirect-uri` (디스코드의 최신 파일로 받는다, 건강기록 작업에는 안 쓴다)
- 참고: 로컬 `application.properties`는 `springdoc.*.enabled=false`라 Swagger를 보려면 `./gradlew bootRun --args='--springdoc.api-docs.enabled=true --springdoc.swagger-ui.enabled=true'`로 띄운다
- 참고: 공용 DB에 v0.14 (3)·(4)(HLTG25 그룹까지)·(5)를 실행함(2026-11-09). 공통코드 번호를 새로 정할 때는 기준 파일 본문뿐 아니라 `db/archive/` 변경 절과 공용 DB도 본다(HLTC0079 중복 사례)
- 참고: 케어 일정 알림 petSn 변경 전에 생긴 알림은 링크가 `/health/calendar` 그대로다
- 참고: 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 참고: 공용 DB에 남은 확인용 데이터: 회원 1(반려동물 3, 검진일 2026-10-31) 검진 포인트 20P(원장 5634), 영수증 179 상태 이력 1364행, 반려동물 2의 관찰 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행. 반려동물 1의 `PET_BDWG`는 17.513(남은 최신 기록 91). 공용 DB의 HLTG21 코드 5개는 실제 코드라 그대로 둔다
- 참고: 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(사람 확인 2026-10-29). 참고범위는 PATCH `refRange`로 덮어쓰거나 비운다(2026-11-02)
- 참고: 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 참고: `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~9999 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 참고: 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
