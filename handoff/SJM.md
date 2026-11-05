# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 반려동물 등록/수정 · 종에 맞지 않는 털색 데이터 정리 — 끝남 (승인 2026-11-06 01:00, 시작 2026-11-06 00:55). 검증 통과 1회차, 사람 확인 완료. 공용 DB 반영은 "다음에 할 것" 맨 위

기능 ID 추가 없음, FN-MYP-003-03에 걸침. Java 코드와 API는 바꾸지 않는다. 시드 SQL만 고친다.

- 현황(시드 `PET` 덤프 집계): 종-털색 불일치는 고양이(PETC0002)+얼룩(PETC0011, 강아지 전용) 55마리뿐. 고양이 전용 털색을 쓴 강아지, 그 밖의 강아지 전용 털색을 쓴 고양이는 없다
- 바꿀 털색: 얼룩 → 젖소(PETC0032, 고양이 전용). 흰 바탕에 검은 얼룩이라 뜻이 가장 가깝다. 55마리를 모두 같은 값으로 바꾼다
- 바꿀 파일: `db/seed/20261028_seed_data.sql`
  - `PET` 덤프(`UNLOCK TABLES;`) 바로 뒤에 ``UPDATE `PET` SET `COLR_CD`='PETC0032' WHERE `PET_SE_CD`='PETC0002' AND `COLR_CD`='PETC0011';`` 한 줄을 넣는다(덤프 행은 고치지 않는다. 앞선 DAY_MSN UPDATE와 같은 방식)
  - 머리말에 "2026-11-06: 고양이+얼룩(강아지 전용) 55마리를 젖소(PETC0032)로 바꾸는 UPDATE를 PET 덤프 아래에 추가. 공용 DB에도 이 UPDATE 하나만 실행한다(팀에 알린 뒤 사람이 DBeaver로)" 한 줄
- 부를 API: 없음
- 완료 조건:
  - 명령: `grep -c "SET .COLR_CD.='PETC0032' WHERE .PET_SE_CD.='PETC0002' AND .COLR_CD.='PETC0011'" db/seed/20261028_seed_data.sql`가 1이고 그 줄이 `PET` 덤프의 `UNLOCK TABLES;`보다 뒤. `PET` INSERT 줄(덤프 행)은 바뀌지 않음(`git diff`에 INSERT 줄 없음). `./gradlew build -q` 통과
  - 화면: 없음(사람 확인: 로컬 DB에 시드를 다시 넣지 않는다. 파일은 grep과 git diff로만 확인한다)
- 공용 DB 반영은 이 마일스톤 밖이다. "다음에 할 것"에 "팀에 알린 뒤 공용 DB에 위 UPDATE 실행(사람, DBeaver)"을 남긴다

새 의존성: 없음

## 다음에 할 것
- 공용 DB 반영(SCR-MYP-003 털색 정리, 사람이 DBeaver로, 시드 전체는 실행하지 않는다):
  1. 팀에 알린다.
  2. 실행 전 개수를 본다: `SELECT COUNT(*) FROM PET WHERE PET_SE_CD='PETC0002' AND COLR_CD='PETC0011';`
  3. 트랜잭션 안에서 `UPDATE PET SET COLR_CD='PETC0032' WHERE PET_SE_CD='PETC0002' AND COLR_CD='PETC0011';`를 실행하고, 영향받은 행 수를 2번과 비교한 뒤 COMMIT 한다.
  4. 탄이 수정 모달의 "얼룩 (강아지 털색)" 안내가 사라졌는지 본다.
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
