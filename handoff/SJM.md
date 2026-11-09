# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 반려동물 등록/수정 · 등록 직후 케어미션 즉시 출제 — 끝남 (승인 2026-11-09 11:14, 시작 2026-11-09 15:00). 검증 통과 1회차, 사람 확인 완료(Postman 등록 직후 미션 조회, 개수 유지 확인, 확인용 반려동물 삭제)

기능 ID 추가 없음, FN-MYP-003-01(등록)과 FN-MYP-020-03에 걸침. 피드백: 새 반려동물은 매시 정각 배치 때까지 최대 1시간 미션이 비어 있다.

- `pet/mapper/CareMissionMapper.java`·`mapper/pet/CareMissionMapper.xml`: `selectIssueTarget(petSn, primaryRoleCd)`(기존 `selectIssueTargets`와 같은 SELECT에 `R.PET_SN = #{petSn}`), `countDayMissions(petSn, msnCrtrYmd)`(`DEL_YN='N'`) 추가
- `pet/service/CareMissionService.java`: `issueFor(Long petSn, LocalDate msnCrtrYmd, boolean observing)` 추가. 오늘 미션이 이미 있으면 건너뛰고, 없으면 `pick`으로 고른 미션을 `insertDayMission`한다. `issue`의 DAY_MSN 만들기·INSERT를 `insertMissions(target, msnCrtrYmd, observing, masters)` 한 메서드로 빼서 둘이 같이 쓴다(대상 선정·pick 재사용)
- `pet/service/PetService.java` `createPet`: `insertBrupRel` 뒤에 `PetCreatedEvent(petSn, 오늘)`를 `EventPublisher`가 아니라 `ApplicationEventPublisher`로 발행한다(알림·메일이 아니라 `PetDeletedEvent`와 같은 도메인 내부 이벤트). 등록 트랜잭션이 커밋된 뒤에 출제하므로 출제 실패가 등록을 롤백시키지 않는다
- `pet/domain/PetCreatedEvent(petSn, msnCrtrYmd)` 새 record, `pet/service/PetCreatedEventListener`: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`로 `careMissionService.issueFor(petSn, msnCrtrYmd, false)`를 부르고, 예외는 잡아 `log.warn`만 남긴다(다음 정각 배치가 채운다). `PetDeletedEvent` 발행·수신 방식을 먼저 보고 같게 맞춘다
- 관찰 중 여부는 `false`로 넘긴다. 방금 등록한 반려동물에는 관찰 항목이 없어 배치 기준(`ONGOING` 관찰 항목 보유)과 결과가 같다. 또 `pet`(1층)이 `health`(2층) `ObservationService`를 부르면 층 규칙(`ArchitectureTest`)에 어긋나므로 부르지 않는다
- 부를 API: `GET /pets/{petSn}/daily-missions`(MYP-020-03·04, SJM, 기존). 새 API 없음, `docs/api/` 초안 수정 없음
- 중복 방지: 출제 전 `countDayMissions`로 확인하고, DB 유니크 `UK_DAY_MSN_1`(PET_SN, MSN_CRTR_YMD, CARE_MSN_SN)이 배치와 동시에 들어오는 경우를 막는다. 배치 `issue`는 이미 `selectIssuedPetSns`로 건너뛰므로 코드 변경 없음
- 테스트: `CareMissionServiceTest`에 `issueFor` 출제(8개)·이미 출제됐으면 건너뜀·`issue`와 `issueFor`를 연달아 불러도 미션 수가 늘지 않음. `PetServiceTest`에 `createPet`이 `PetCreatedEvent`를 발행하는지. 리스너 테스트에 "`issueFor`에서 예외가 나도 리스너는 던지지 않고 로그만 남김"과 "예외가 나도 반려동물 등록은 성공"(통합: 등록 응답이 오고 DB에 PET이 남음)
- 완료 조건:
  - 명령: `./gradlew build -q` 통과
  - 화면: 반려동물을 등록하면 곧바로 `GET /pets/{petSn}/daily-missions`가 미션 8개를 돌려준다. 다음 정각 배치 뒤에도 그 아이의 미션 수가 그대로다. 확인용 반려동물은 화면의 삭제 기능으로 지운다
- 프론트 연결: 없음(응답이 그대로라 마이페이지 홈이 이미 부르는 API를 그대로 쓴다)

새 의존성: 없음

## 다음에 할 것
- 확인: 등록 직후 출제(`PetCreatedEventListener`)가 UK_DAY_MSN_1 등으로 실패하면 `issueFor`가 리스너의 REQUIRES_NEW 트랜잭션에 합류해 롤백 전용이 되어, 커밋 때 `UnexpectedRollbackException` 로그가 함께 찍힐 수 있다(등록 응답에는 영향 없음, 다음 정각 배치가 채움). 거슬리면 `issueFor`를 `REQUIRES_NEW`로 바꾸는 것을 고려
- Postman 컬렉션: 3번 회원이 정지(`MEMBER_INACTIVE`)라 그 회원을 쓰는 변수(`mbrSnB`·`emailB`·`delCoMbrSn`·`delCoEmail`·`invReceiverMbrSn`·`infoOtherMbrSn`)가 있는 폴더는 403. 돌릴 때 정상 회원으로 바꾼다(이메일은 `GET /members/me`의 `acntEml`)
- YAY와 정하기(SCR-MYP-040): 본인 확인(6-1)을 통과했는지 `PATCH /members/me`에서 서버가 검사하지 않는다. `POST /members/me/password-verification`을 만들 때 통과 표시를 어떻게 남길지 함께 정한다
- 화면 확인 필요(FN-MYP-002-08): 알림함 COM-004에 공동양육 연동 해제 알림(CO_PARENTING, 이동 경로 `/mypage/pets`)이 보이는지. 백엔드는 `PetService.java:273`에서 끝
- 팀 확인: 공동양육 강제 해제·자진 해제(`PetService.unlinkCaregiver`)에는 알림 발행이 없다. 기획이 이 경로의 알림까지 요구하는지 정하기
- JAJ에게 전달(SCR-MYP-002): `pet/domain/PetDeletedEvent(petSn)`를 `@TransactionalEventListener(phase = BEFORE_COMMIT)`(또는 `@EventListener`)로 받아 DAILY_HLTH_RCD·OBSRV_ARTCL·OBSRV_CYCL·SCHDL·CKUP_RCD(+하위)·REPT_PRVNT_MNG·CARE_ARTCL을 `DEL_YN='Y'`로 바꾸는 리스너, health 조회의 `CKUP_RCD.DEL_YN='N'` 조건. AFTER_COMMIT이면 실패해도 삭제가 커밋되므로 안 된다
- PR 본문에 "DB 변경 있음: `db/migration/20261030_01_add_del_yn_for_pet_delete.sql`(공용 DB `pawever` 2026-10-30 적용 완료)"를 적는다
- LJS에게 알리기(SCR-MYP-004): `scheduler/CaregiverFake` → `PetService.searchCaregiverMbrSns(petSn)`, `expense/CaregiverPetFake` → `PetService.searchMyPets(mbrSn)`의 petSn·petNm으로 바꾸기
- 화면 확인 필요(FN-MYP-030-08): 알림 목록 COM-004에 가계부 한도 알림이 보이는지. 백엔드는 `ExpenseService.java:313·316` 발행으로 끝
- LJS에게 알리기: `ReceiptService.markSavedByExpense`를 더함(인식 실패 후 직접 입력도 영수증을 저장완료로, LJS 파일)
- 배포 후 케어미션 출제·정산 배치가 느리면 개선: 지금은 한 행씩 INSERT·UPDATE하고 전체가 한 트랜잭션이다(로컬 시드 약 960마리로 30분, 2026-10-27 확인). 여러 행을 한 번에 넣고 묶음마다 커밋하도록 `/start`로 기획. 배치는 prod에서만 켜지므로 그 전에는 하지 않는다
- LJS에게 `MasterSeeder` 미션 목록 변경 알리기(21개로 바뀜, `CharacterSeeder`는 그대로)
- KSJ·LJS에게 알리기(SCR-MYP-020): KSJ 펫이야기 글·댓글 → `PetService.reflectRepresentative`(아직 `community`에 호출 없음), LJS `pet/service/PointFake` → `PointService.use`(아직 `ItemService`가 `PointFake`를 씀)
- JAJ 확인: 케어미션 `reflect` 중 일정은 직접 입력(`DIRECT`)일 때만 반영한다(`CareScheduleService.java:87`). 명세와 같은지 확인
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
