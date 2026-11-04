# JAJ 핸드오프

## 지금 하는 것

SCR-HLT-002 건강 일지 · 메인 건강 지표 — 끝남: 검증 통과(1회차), 사람 확인 통과. `GET /api/pets/{petSn}/health-indicators`가 기준일·조회 시각과 체중·식사량·음수량·산책·혈액검사(CREA) 위젯 값을 준다. 기록 없는 지표는 null, 비교 대상 없으면 대비 null (승인 2026-11-04 13:51, 시작 2026-11-04 13:45)

## 다음에 할 것

- 연결 남음: FN-MAN-005·006 (프론트 연결 메인 건강 대시보드 위젯 5종·기준일 표시, 프론트 저장소에서 /start — 화면은 YAY 담당이라 YAY에게 알린다). `GET /api/pets/{petSn}/health-indicators`, 응답 `HealthIndicatorResponse`(`docs/api/health.md`). "최근 업데이트 시각"은 `inqDt`, 지표가 null이면 "기록이 없어요", `prevDiff`가 null이면 비교 문구를 숨긴다
- 참고: 사람 확인용 식사 기록 55426~55428(반려동물 1)은 `DEL_YN='Y'`로 지웠다. 그날 미션 변화는 없었다
- 참고: 사람 확인용 일정 4926~4928(회원 1, 반려동물 1·2)은 `DEL_YN='Y'`로 지웠다. 그날 두 반려동물에 해당 미션이 없어 미션·포인트 변화는 없었다
- 참고: 검진 포인트 확인 중 회원 1(반려동물 3, 검진일 2026-10-31)에 20P(원장 5634)가 공용 DB에 남는다. 검진기록 1011~1013은 지웠다
- 연결 남음: FN-HLT-002-31 (프론트 연결 검진기록 등록 모달을 영수증 실패 화면의 [직접 입력]으로 열었을 때 저장을 `POST /api/receipts/{recptSn}/checkups?petSn=`로, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 영수증 [직접 입력] 저장 연결"). 요청은 직접 등록 Body에 `idmptDmndId`를 더한다. 같은 요청 재전송은 409 `RECEIPT_ALREADY_SAVED`, 응답은 `CheckupResponse`(201)
- 참고: 이번 확인 중 영수증 179(회원 48)를 원래 상태(AIMC0002·샘플 멱등 ID)로 되돌렸고, 상태 이력 1364행은 공용 DB에 남는다

- 연결 남음: FN-HLT-002-19·23·24 (프론트 연결 지속 관찰 회차 수정 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 지속 관찰 회차 수정 비우기 연결"). `updateObservationCycle`이 비운 칸 이름을 `clearFields`로 보내고(반복 키 또는 쉼표, 손대지 않은 칸은 안 보냄), 수정 모달 사진 칸은 기존 사진 미리보기 + [사진 바꾸기]만, 보조 문구 "사진은 바꿀 수만 있어요"(기능명세서 FN-HLT-002-22에도 반영). 400은 `data[0].field`가 `clearFields[i]`·`clearFieldsDistinct`

- 연결 남음: FN-HLT-007-02·03 (프론트 연결 `LabInput.jsx`·`BloodResultForm.jsx` 참고범위 수정·비우기, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-007 검사 수치 입력 · 참고범위 비우기 연결"). 수정 저장 때 하한·상한을 `refRange`로 묶어 보내고(빈 칸은 null, 안 보내면 유지), "참고범위는 수정에서 지울 수 없어요" 막기를 지운다. 한쪽만 입력하거나 최소 > 최대이면 저장 전에 막는다("참고범위 최소는 최대보다 클 수 없어요", 등록·수정 모두). 범위 없으면 "참고범위 없음"·▲▼ 판정 없음·추이 밴드 없음(밴드는 가장 최근 회차 범위). PATCH 400은 `data[0].field`가 `refRange.paired`·`refRange.ordered`, 등록 400은 필드 목록 없음

- 연결 남음: FN-HLT-003-12 (프론트 연결 케어리스트 기록 내용 아이콘 취소·투약 [마지막 1회 취소], 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 투약 취소 API 연결"). 기록 내용 아이콘은 `DELETE /pets/{petSn}/care-completions/{careCmptnHstrySn}`(투약도 그날 전부), [마지막 1회 취소]는 `DELETE /pets/{petSn}/care-items/{careArtclSn}/today-completion/last`(투약 아니면 400, 오늘 기록 없으면 404). 기록 내용 조회는 투약을 N회 채운 날만 준다
- 알림: AIM 담당(LJS)에게 `searchNamesByPets`가 이제 기본 항목을 만들고 새 트랜잭션(REQUIRES_NEW)으로 돈다고 전한다
- 참고: 반려동물 삭제는 `HealthPetDeleteListener`(PetDeletedEvent, BEFORE_COMMIT)가 health 12개 테이블을 `DEL_YN='Y'`(MDFR_ID SYSTEM)로 바꾼다. 검진기록 쪽은 `SCHDL.PET_SN`으로 찾고 `S.DEL_YN`은 보지 않는다. 새 health 테이블을 만들면 이 리스너에도 넣는다
- 연결 남음: FN-HLT-011-01~10 (프론트 연결 종합 건강기록 화면 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-011 종합 건강기록 · API 연결"). 반려동물 전체 조회는 첨부·검사 수치·처방약·반복 예방 목록에서 `ckupRcdSn`을 빼고 부른다. 반복 예방 전체 조회에만 `hsptlNm`이 채워진다
- 연결 남음: FN-HLT-002-15~25·32 (프론트 연결 건강 일지 지속 관찰 탭·관찰 기록 등록 모달 A/B, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 지속 관찰 API 연결")
- 참고: 관찰 확인용 데이터를 지워도 업로드 파일 행·`OBSRV_ARTCL_STTS_HSTRY` 행은 공용 DB에 남는다(반려동물 2)
- 연결 남음: FN-HLT-002-01·09~13 (프론트 연결 건강 일지 반려동물 드롭다운·건강기록 변화 그래프·오늘의 기록 카드·[오늘 기록추가] 입력 영역·최근 건강기록 표·상세 기록 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 일상 기록 API 연결"). 서버는 날짜별로 묶지 않는다. 그래프는 `size=14` 뒤 가장 이른 날짜를 `fromYmd`로 다시 불러 하루 합계를 맞춘다
- 연결 남음: FN-HLT-007-01~06 (프론트 연결 검사 수치 입력 페이지 [저장하기]·수정 진입·분석 결과 검수(분석은 화면이 Python `POST /ai/checkups/lab-results` 직접)·[다시 분석하기]·[직접 입력하기], 검진상세 [＋ 검사 수치 추가], 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-007 검사 수치 입력 · API 연결")
- 참고: 저장 `POST inspection-values`는 전부 건너뛰어도 201(빈 목록)을 돌려준다(기획의 200에서 바꿈, 사람 확인 2026-10-29). 참고범위는 PATCH `refRange`로 덮어쓰거나 비운다(2026-11-02)
- 연결 남음: FN-HLT-005-01~03·05·11~15 (프론트 연결 검진상세 페이지 전체, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-005 검진상세 · API 연결")
- 참고: 처방약 약품명을 바꿔도 이미 만든 투약 항목 이름은 그대로다
- 연결 남음: FN-HLT-003-01·12·13·21~24·38 (프론트 연결 케어리스트 탭 드롭다운·목록·기록 내용·완료 토글·★ 토글·관리 항목 추가 모달, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 토탈 케어리스트 API 연결"). 메인페이지 MAN-009는 YAY가 `PUT care-item-exposures`를 쓴다
- 참고: `CareItemCreateRequest.cycleDrctInptDayCnt`의 1~9999 검사가 일상관리 요청에도 걸린다(0을 보내면 400). AI 카드는 일상관리일 때 이 필드를 보내지 않는다
- 참고: 시드 기본 항목 이름("빗질하기", "발톱 정리" 등)이 `BasicCareItem` 이름과 달라 시드 반려동물의 기본 항목 완료는 케어미션에 반영되지 않는다(시드는 고치지 않기로 함)
- 연결 남음: FN-HLT-002-05·06·14·26~31·46 (프론트 연결 검진기록 탭, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-002 건강 일지 · 검진기록 API 연결"). 목록 건수 필드는 `totalElements`(기획의 `totalCount` 아님)
- 연결 남음: 추가-1 (프론트 연결 `features/health/api.js` 등 일정 화면, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 일정 API 연결")
- 연결 남음: FN-HLT-003-39 (반려동물 목록 API, SJM)
- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장 화면 SCR-AIM-012, LJS)
- HLTG21(캘린더 뱃지 색상) 하위 코드가 `db/` 공통코드에 없다. 코드 값을 정해 `db/migration`에 넣는 것을 팀과 맞춘다(그 전엔 색상 저장 불가)
- `application.properties`에 빠진 키: `app.front-base-url`, `app.seed.password` (디스코드의 최신 파일로 받는다)
