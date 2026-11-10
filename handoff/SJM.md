# SJM 핸드오프

## 지금 하는 것
마이페이지 시드 정리 · PET·DAILY_HLTH_RCD 시드 원본 정리 — 끝남 (승인 2026-11-10 16:04). 검증 통과 1회차, 사람 확인 완료(빈 DB에 create_tables → base_data → seed 순서로 넣고 SQL 10단계 확인 쿼리 기대값 0/0/4/0/N/0/0/0/0/0 일치)

기능 ID 없음(시드 SQL 정리). 근거: 마이페이지 시드 정리 SQL(사람이 준 파일) 1~9단계를 덤프에 그대로 적용. 대상은 `DEL_YN='N'`·`RGTR_ID='SEED'` 행만.

- `db/seed/20261028_seed_data.sql`: PET 줄(593)과 DAILY_HLTH_RCD 줄(241~247)만 파싱 스크립트로 바꾼다. 품종명은 덤프 VRTY 줄 기준. 1단계 품종(코숏 믹스→VRTY_SN 24, 진도 믹스→진돗개 믹스, `믹스`는 시드에 없어 할 일 없음, 믹스묘·푸들 믹스·믹스견은 직접 입력 유지), 2·3단계 털색(품종명+MOD(PET_SN) 규칙 그대로), 4단계 털색 직접 입력 4마리(2·3단계 뒤), 5~8단계 체중 소수 첫째 자리·프로필 체중=최근 기록. 머리말에 날짜별 이력 한 줄(공용 DB는 2026-11-10 반영 완료)
- `db/20261109_insert_base_data_v0.14.sql`: PETC0011(얼룩) INSERT에 `USE_YN='N'`, 맨 위 변경 절에 새 항목(UPDATE COM_CD … PETC0011) 주석으로 추가. 기존 항목은 고치지 않는다
- 변환 스크립트는 scratchpad에만 두고 커밋하지 않는다. SQL 파일은 저장소로 복사하지 않는다
- 완료 조건:
  - 명령: `./gradlew build -q` 통과
  - 확인: SQL 10단계 확인 항목 기대값 0/0/4/0/N/0/0/0/0/0, 줄 수 1146 유지, 바꾼 컬럼 외 바이트 동일, SQL 규칙 재계산과 PET_SN별 일치, 재실행해도 결과 같음
- 프론트 연결: 없음
새 의존성: 없음

## 다음에 할 것
- 팀에 알린다(시드): `db/seed/20261028_seed_data.sql`의 PET·체중 기록(HLTC0014)을 정리했고(품종 직접 입력명, 품종별 털색, 얼룩 미사용, 체중 소수 첫째 자리) `db/20261109_insert_base_data_v0.14.sql` 변경 절 v0.14 (8)로 얼룩(PETC0011)을 사용 중지했다. 공용 DB는 2026-11-10 반영 완료. 이미 시드를 넣은 로컬 DB는 시드를 다시 넣지 말고 팀에서 받은 정리 SQL로 맞춘다
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
