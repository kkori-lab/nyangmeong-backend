# SJM 핸드오프

## 지금 하는 것
SCR-MYP-004 공동양육자 목록 — 구현 중 (승인 2026-10-29 04:43, 시작 2026-10-29 04:29)
- 초안 고치기(구현 전): `docs/api/pet.md` GET /pets/{petSn}/caregivers 설명과 CaregiverSummary 설명을 "주양육자에게는 전원, 공동양육자에게는 주양육자 + 본인"으로, `linkYmd`(LocalDate, Y, 연동일 "07-02 연동", 마지막 `PETC0024` 이력 REG_DT, 없으면 BRUP_REL.REG_DT) 추가. 이유: 화면설계서 4-2가 공동양육자에게도 주양육자 카드(FN-MYP-004-03)를 보이고, 카드마다 연동일을 표시한다. 권한("조회+수정")은 역할로 정해져 필드를 두지 않는다. `PetResponse.caregivers[]`도 같은 모양이라 함께 고친다
- FN-MYP-004-01 가족 목록: `PetController`에 `GET /pets/{petSn}/caregivers`, `PetService.searchCaregivers`(PetAuthorizer.checkCaregiver), `PetMapper.selectCaregivers`에 `LINK_YMD` 추가(PetMapper.xml), `CaregiverSummary.linkYmd`. 주양육자에게는 연동 중(USE_YN='Y') 전원, 주양육자 첫 줄. 명령: `./gradlew build -q`(PetServiceTest·PetControllerTest에 추가). 화면: 주양육자 토큰으로 조회하면 주양육자 1줄 + 공동양육자 전원이 연동일과 함께 온다. 프론트 연결: SCR-MYP-002 슬라이드 카드 가족 영역·[✕] (프론트 저장소에서 /start)
- FN-MYP-004-03 주양육자 표시: 위 목록 첫 줄이 `brupRoleCd=PETC0018`. 명령: 같은 테스트. 화면: 주양육자·공동양육자 어느 쪽으로 조회해도 첫 줄이 주양육자다
- FN-MYP-004-04 내 연동 상태: 공동양육자에게는 주양육자 + 본인 1건만(다른 공동양육자 제외). `PetService.toResponse`의 거르기도 같은 규칙으로 바꿔 GET /pets/{petSn}과 맞춘다. 명령: 같은 테스트. 화면: 공동양육자 A 토큰으로 조회하면 공동양육자 B가 안 보인다. 양육관계 없는 회원은 403(PET_ACCESS_DENIED)
- FN-MYP-006-02 자진 해제 + FN-MYP-004-01 강제 해제: `DELETE /pets/{petSn}/caregivers/{brupRelSn}`, `PetService.unlinkCaregiver`(@Transactional). 대상 BRUP_REL이 그 반려동물의 연동 중 공동양육자 행이어야 함(아니면 404 CAREGIVER_NOT_FOUND, 주양육자 행은 해제 불가). 주양육자는 누구나 해제(강제, `PETC0026`), 공동양육자는 자기 행만(자진, `PETC0025`, 남의 행이면 403 PET_PRIMARY_ONLY). `BRUP_REL` USE_YN='N'·RPRS_PET_YN='N'·MDFR_ID 갱신, `BRUP_LINK_STTS_HSTRY`에 대상 회원·유형 INSERT(PetMapper `updateBrupRelUnlink`·`insertLinkHistory`, 연동변경유형 enum `LinkChangeType` pet/domain). 명령: 같은 테스트. 화면: 해제 뒤 USE_YN='N'이고 이력 1행, 해제된 회원이 GET /pets/{petSn}에 403. 프론트 연결: SCR-MYP-006 확인 팝업·토스트 (프론트 저장소에서 /start)
- FN-MYP-006-04 목록 즉시 반영: 해제 뒤 GET /pets(해제한 공동양육자 목록에서 빠짐, 주양육자의 `caregiverCount` 감소)·GET /pets/{petSn}/caregivers가 바로 바뀐다(캐시 없음, 같은 조회 조건). 명령: 같은 테스트. 화면: 해제 직후 두 목록 재조회
- 함께 만드는 것: `PetService.searchCaregiverMbrSns(petSn)` — 주양육자 + 연동 중 공동양육자 회원 번호(삭제된 반려동물이면 빈 목록), `scheduler/CaregiverFake` 대체용. 연동 반려동물 목록은 이미 있는 `searchMyPets`(petSn·petNm)로 `expense/CaregiverPetFake`를 대체한다. 가짜 연결은 LJS 파일이라 "다음에 할 것"에 남긴다. 명령: PetServiceTest
- 알림: 명세에 해제 알림 기능 ID가 없어 이번에 발행하지 않는다
새 의존성: 없음
## 다음에 할 것
- 연결 남음: FN-MYP-030-01~06·09~21 프론트 SCR-MYP-030 가계부 기본 화면·전체 내역·편집 모드·한도 설정·삭제 모달·월 피커(프론트 저장소에서 /start). API: `GET /expenses/statistics`·`/expenses`·`/totals`·`/months`·`/pets`, `PATCH`·`DELETE /expenses/{expndSn}`, `DELETE /expenses?expndSn=`, `POST /expenses/bulk-delete`, `PUT /members/me/expense-limit`
- 연결 남음: FN-MYP-031-02·03·05·06 프론트 SCR-MYP-031 직접 입력·인식 결과 확인 저장·실패 후 직접 입력의 영수증 연결·중복 지출 확인 모달(프론트 저장소에서 /start)
- 연결 남음: FN-MYP-031-05 LJS 영수증 저장 `ExpenseFake` → `ExpenseService.create`·`findDuplicates`(가짜와 같은 모양), FN-MYP-030-08 LJS 알림 목록 COM-004에 가계부 한도 알림 표시
- LJS에게 알리기: `ReceiptService.markSavedByExpense`를 더함(인식 실패 후 직접 입력도 영수증을 저장완료로, LJS 파일)
- 배포 후 케어미션 출제·정산 배치가 느리면 개선: 지금은 한 행씩 INSERT·UPDATE하고 전체가 한 트랜잭션이다(로컬 시드 약 960마리로 30분, 2026-10-27 확인). 여러 행을 한 번에 넣고 묶음마다 커밋하도록 `/start`로 기획. 배치는 prod에서만 켜지므로 그 전에는 하지 않는다
- LJS에게 `MasterSeeder` 미션 목록 변경 알리기(21개로 바뀜, `CharacterSeeder`는 그대로)
- 연결 남음: 프론트 SCR-MYP-020 포인트 카드·요약·내역·케어미션 탭과 SCR-MYP-001 요약(프론트 저장소에서 /start, SCR-MYP-003은 체중 잠금은 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 판단, 기본 선택 동물은 `rprsPetYn`, 털색 드롭다운 설명), JAJ 건강 일지 기록·케어리스트 기본 항목 체크·일정·관찰 기록 → `reflect`, 검진기록·영수증 빠른 등록 → `earnCheckupPoint`, 관찰 중 반려동물 조회(`scheduler/ObservationFake` 대체), KSJ 펫이야기 글·댓글 → `reflectRepresentative`, LJS `PointFake` → `PointService.use`
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
