# SJM 핸드오프

## 지금 하는 것
SCR-MYP-002 반려동물 삭제 — 기획 (시작 2026-10-30 00:02)
- 초안 고치기(구현 전): `docs/api/issues.md` 마이페이지 3번에 이번 결정을 적는다(`docs/api/pet.md` 행은 그대로). 삭제는 `pet` 도메인 안에서만 처리한다: PET·PIXEL_CHAR·HLD_ITEM `DEL_YN='Y'`, 공동양육 연동 해제·알림, 대표 승계. 건강기록·일정·관찰·반복 예방·AI 대화방(위층 health·ai)은 지우지 않는다. `PetAuthorizer`가 `PET.DEL_YN='N'`으로 막아 다시 볼 수 없고, pet(1층)은 위층을 부를 수 없다. 당일 미션(DAY_MSN, DEL_YN 없음)은 행을 지우지 않는다. 조회·출제·정산 SQL이 이미 `P.DEL_YN='N'`으로 뺀다. 지출은 유지(`petDelYn` 이미 있음). 대기 초대의 그 반려동물은 초대 조회·수락 SQL이 이미 뺀다
- FN-MYP-002-07 삭제(확인 팝업은 프론트): `PetController` `DELETE /pets/{petSn}` → `ApiResponse` 데이터 없음, `PetService.deletePet(mbrSn, petSn)`(@Transactional). `petAuthorizer.checkPrimary`(없거나 삭제됨 404 PET_NOT_FOUND, 공동양육자 403 PET_PRIMARY_ONLY) → `PetMapper.updatePetDelete`(DEL_YN='Y', 0행이면 404) → `PixelCharacterMapper`에 `updatePixelCharDeleteByPet`·`updateHeldItemDeleteByPet` 추가(캐릭터 없으면 0행이어도 정상). 진행 중 캐릭터 생성 작업은 `CharacterCreationWorker`가 반려동물 삭제를 보면 결과를 저장하지 않는지 확인하고, 아니면 건너뛰는 검사를 넣는다. 대표였으면 승계: 남은 주양육 동물 중 `searchMyPets` 순서(등록 순) 첫 마리를 `updateRepresentativeOn`, 없으면 대표 없음. 명령: `./gradlew build -q`(PetServiceTest·PetControllerTest에 삭제 케이스 추가). 화면: 주양육자 토큰으로 DELETE → 200, 다시 `GET /pets`에 안 나오고 `GET /pets/{petSn}` 404, 공동양육자 토큰 DELETE → 403, 대표 동물 삭제 뒤 다음 주양육 동물이 `rprsPetYn=true`, `GET /expenses`의 그 지출이 `petDelYn=true`로 남음. 프론트 연결: SCR-MYP-002 ⋮ 메뉴 [반려동물 삭제]·2-3 삭제 확인 팝업·2-4 등록 카드 복귀(프론트 저장소에서 /start)
- FN-MYP-002-08 연동 자동 해제: 같은 트랜잭션에서 그 반려동물의 연동 중 공동양육자(BRUP_REL CCG, USE_YN='Y') 전부 `updateBrupRelUnlink` + 연동 이력 `LinkChangeType.PET_DELETED`(PETC0027) 1행씩, 각 공동양육자에게 `EventPublisher.publish(NotificationEvent(CO_PARENTING, "<반려동물 이름>의 기록이 정리되어 공동양육 연동이 해제되었어요", mvmnPath `/mypage/pets`))`. 주양육자 BRUP_REL 행은 그대로 둔다(PET.DEL_YN으로 빠짐). 회원 탈퇴는 대상 아님(YAY 소관, 손대지 않음). 명령: 같은 테스트(공동양육자 2명 → 해제 2행·이력 2행·이벤트 2건, 없으면 0건). 화면: 삭제 뒤 공동양육자 토큰 `GET /pets`에 안 나오고 알림함(`GET /notifications`, LJS)에 해제 알림 1건
- 의존 방향: `PetService`가 같은 도메인 `PetMapper`·`PixelCharacterMapper`만 쓴다. 다른 도메인 호출 없음
새 의존성: 없음
## 다음에 할 것
- 연결 남음(SCR-MYP-005): FN-MYP-005-01·02 프론트 5-1 초대 모달, FN-MYP-005-03·04 프론트 SCR-MYP-002 하단 보낸 초대, FN-MYP-005-05·07·08·09·10 프론트 5-3 수락·5-4 거절 모달·5-5 이메일 불일치·5-6 만료·종료(프론트 저장소에서 /start). API: `POST·GET /caregiver-invitations`, `GET /caregiver-invitations/{jntBrupInvtnSn}`, `POST …/accept`(petSns)·`…/reject`. 초대 링크는 프론트 `/mypage/invitations/{jntBrupInvtnSn}`
- PR 본문에 "DB 변경 있음: `db/migration/20261029_01_add_invtn_trgt_pet_acpt_yn.sql`(공용 DB `pawever` 2026-10-29 적용 완료)"를 적는다
- 연결 남음(SCR-MYP-004): FN-MYP-004-01·03·04 프론트 SCR-MYP-002 슬라이드 카드 가족 영역·[✕], FN-MYP-006-02·04 프론트 SCR-MYP-006 확인 팝업·토스트(프론트 저장소에서 /start). API: `GET /pets/{petSn}/caregivers`(`linkYmd` 추가), `DELETE /pets/{petSn}/caregivers/{brupRelSn}`
- LJS에게 알리기(SCR-MYP-004): `scheduler/CaregiverFake` → `PetService.searchCaregiverMbrSns(petSn)`, `expense/CaregiverPetFake` → `PetService.searchMyPets(mbrSn)`의 petSn·petNm으로 바꾸기
- 연결 남음: FN-MYP-030-01~06·09~21 프론트 SCR-MYP-030 가계부 기본 화면·전체 내역·편집 모드·한도 설정·삭제 모달·월 피커(프론트 저장소에서 /start). API: `GET /expenses/statistics`·`/expenses`·`/totals`·`/months`·`/pets`, `PATCH`·`DELETE /expenses/{expndSn}`, `DELETE /expenses?expndSn=`, `POST /expenses/bulk-delete`, `PUT /members/me/expense-limit`
- 연결 남음: FN-MYP-031-02·03·05·06 프론트 SCR-MYP-031 직접 입력·인식 결과 확인 저장·실패 후 직접 입력의 영수증 연결·중복 지출 확인 모달(프론트 저장소에서 /start)
- 연결 남음: FN-MYP-031-05 LJS 영수증 저장 `ExpenseFake` → `ExpenseService.create`·`findDuplicates`(가짜와 같은 모양), FN-MYP-030-08 LJS 알림 목록 COM-004에 가계부 한도 알림 표시
- LJS에게 알리기: `ReceiptService.markSavedByExpense`를 더함(인식 실패 후 직접 입력도 영수증을 저장완료로, LJS 파일)
- 배포 후 케어미션 출제·정산 배치가 느리면 개선: 지금은 한 행씩 INSERT·UPDATE하고 전체가 한 트랜잭션이다(로컬 시드 약 960마리로 30분, 2026-10-27 확인). 여러 행을 한 번에 넣고 묶음마다 커밋하도록 `/start`로 기획. 배치는 prod에서만 켜지므로 그 전에는 하지 않는다
- LJS에게 `MasterSeeder` 미션 목록 변경 알리기(21개로 바뀜, `CharacterSeeder`는 그대로)
- 연결 남음: 프론트 SCR-MYP-020 포인트 카드·요약·내역·케어미션 탭과 SCR-MYP-001 요약(프론트 저장소에서 /start, SCR-MYP-003은 체중 잠금은 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 판단, 기본 선택 동물은 `rprsPetYn`, 털색 드롭다운 설명), JAJ 건강 일지 기록·케어리스트 기본 항목 체크·일정·관찰 기록 → `reflect`, 검진기록·영수증 빠른 등록 → `earnCheckupPoint`, 관찰 중 반려동물 조회(`scheduler/ObservationFake` 대체), KSJ 펫이야기 글·댓글 → `reflectRepresentative`, LJS `PointFake` → `PointService.use`
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
