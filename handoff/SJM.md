# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 반려동물 등록/수정 · 종에 맞지 않는 털색 코드 거절 — 끝남 (승인 2026-11-05 23:16, 시작 2026-11-05 23:08). 검증 통과 1회차, 사람 확인 완료(Swagger 대신 브라우저 콘솔: 강아지+PETC0014·고양이+PETC0011 등록 400, 고양이+PETC0017·PETC0005 등록 201, 강아지→고양이 수정 400, 테스트 동물 3마리 삭제 후 8마리 유지)

등록(`POST /api/pets`)·수정(`PATCH /api/pets/{petSn}`)에서 반려동물 구분과 맞지 않는 털색 코드를 거절한다. 명세 FN-MYP-003-03은 "구분에 따라 선택지가 분기"라고만 적고 서버 검증은 없다. 프론트 수정 모달은 이미 막는다(끝남).

| 기능 | 바꿀 파일 | 완료 조건 |
| --- | --- | --- |
| FN-MYP-003-03 털색 선택(등록·수정 서버 검증) | `pet/domain/PetColor.java`, `pet/service/PetService.java`, `pet/service/PetServiceTest.java` | 명령: `./gradlew build -q` 통과. 화면: Swagger로 강아지(`PETC0001`)+`PETC0014` 등록 → 400 `INVALID_INPUT_VALUE`, 고양이(`PETC0002`)+`PETC0011` 등록 → 400, 고양이+`PETC0017`(직접 입력)·`PETC0005`(공통) → 201. 수정에서 `petSeCd`만 고양이로 바꿔 기존 강아지 전용 털색과 어긋나면 400 |

- 부를 API: 없음(공통코드 그룹 검사는 기존 `checkCode` 그대로). 경로·DTO 변경 없음, `docs/api/` 초안 수정 없음
- 결정 1(종별 목록 위치): `PetColor` enum에 `DOG_ONLY`(PETC0010·0011·0012·0028·0029·0030·0031), `CAT_ONLY`(PETC0014·0015·0016·0032·0033·0034) 코드 집합과 `static boolean fits(String petSeCd, String colrCd)`를 둔다. 나머지(PETC0005~0009·0013·0017)는 공통으로 어느 종에서나 받는다. `COM_CD_EXPLN`("강아지 전용") 글자를 읽어 판단하지 않는다(설명 문구는 바뀔 수 있어 조용히 틀어진다)
- 결정 2(명세와 DB 차이): 명세의 강아지 목록에는 "얼룩"(PETC0011)이 없지만 DB 기준 데이터는 `강아지 전용`이다. DB와 마일스톤 완료 조건대로 PETC0011을 강아지 전용으로 둔다(고양이에는 거절)
- 결정 3(오류 코드): 기존 털색 검증처럼 `INVALID_INPUT_VALUE`(400)를 쓴다. 새 `ErrorCode`를 만들지 않는다(프론트가 이미 막으므로 화면 문구가 따로 필요 없다)
- 결정 4(검사 시점): `checkPet`에서 `checkCode(COLOR)` 다음에 저장 직전 최종 값(구분+털색)으로 검사한다. 그래서 수정 요청에 `petSeCd`나 `colrCd` 중 하나만 와도 합친 결과가 어긋나면 거절한다. 시드의 고양이+PETC0011 55마리는 털색을 바꾸지 않고 다른 항목만 고쳐도 거절된다. 프론트 수정 모달이 털색을 다시 고르게 하므로 그대로 두고, 데이터 정리는 대기 중인 "털색 데이터 정리" 마일스톤에서 한다
- 테스트(`PetServiceTest`): 등록 — 강아지+고양이 전용 거절 / 고양이+강아지 전용(PETC0011) 거절 / 고양이+직접 입력·공통 통과. 수정 — `petSeCd`만 바꿔 기존 털색과 어긋나면 거절 / 털색만 바꿔 기존 구분과 어긋나면 거절. 거절이면 `insertPet`·`updatePet`을 부르지 않는다
- 프론트 연결: 없음(수정 모달 막기는 이미 끝남)

새 의존성: 없음

## 다음에 할 것
- 정리(이 파일): 아래 "프론트 저장소에서 /start" 연결 남음 줄이 대부분 이미 연결된 것으로 보인다. 프론트 코드(`../nyangmeong-frontend`)와 대조해 연결된 줄은 지우고, 안 된 것만 남긴다
- 연결 남음(SCR-MYP-030, FN-MYP-030-18·19·21): 프론트 SCR-MYP-030 편집 모드에서 전체 선택 뒤 해제한 번호를 `excludeExpndSn`으로 보내고 `expectedCnt`를 "조건 건수 − 해제한 수"로 바꾸기. 409 `EXPENSE_BULK_DELETE_CHANGED`면 목록을 다시 불러오게 안내(프론트 저장소에서 /start)
- 연결 남음(SCR-MYP-002 목록 응답 보강): 프론트 SCR-MYP-002 슬라이드의 "(중성화 완료)"·"정상 a~bkg"를 상세 조회 대신 `GET /pets`의 `nutrYn`·`minBdwg`·`maxBdwg`로 채우기(프론트 저장소에서 /start)
- Postman 컬렉션: 3번 회원이 정지(`MEMBER_INACTIVE`)라 그 회원을 쓰는 변수(`mbrSnB`·`emailB`·`delCoMbrSn`·`delCoEmail`·`invReceiverMbrSn`·`infoOtherMbrSn`)가 있는 폴더는 403. 돌릴 때 정상 회원으로 바꾼다(이메일은 `GET /members/me`의 `acntEml`)
- 연결 남음(SCR-MYP-005 추가-2): 프론트 5-1 초대 모달의 409 처리를 `data.linkedPets`("이미 ○○의 가족이에요")·`data.pendingPets`("이미 초대를 보냈어요 (MM-DD에 만료돼요)") 목록 기준으로 바꾸기(프론트 저장소에서 /start). 예전 단일 `data.petSn·petNm·invtnExpryDt`는 없어졌다
- 연결 남음(SCR-MYP-040): FN-MYP-040-02·03·08·09·13 프론트 SCR-MYP-040 6-2·6-3 프로필 요약·계정 정보, FN-MYP-040-02·05·08·11 프론트 6-2·6-3 사진 변경·기본 이미지로·휴대폰 번호 [수정]·[저장](프론트 저장소에서 /start). API: `GET /members/me`(`MemberResponse`에 `joinDt`·`lastLgnDt` 추가), `PATCH /members/me`(multipart `mblTelno`·`profilePhoto`·`profilePhotoReset`, 다른 회원 번호 409 PHONE_ALREADY_USED, 사진과 되돌리기 함께 400)
- YAY와 정하기(SCR-MYP-040): 본인 확인(6-1)을 통과했는지 `PATCH /members/me`에서 서버가 검사하지 않는다. `POST /members/me/password-verification`을 만들 때 통과 표시를 어떻게 남길지 함께 정한다
- 연결 남음(SCR-MYP-002): FN-MYP-002-07 프론트 SCR-MYP-002 ⋮ 메뉴 [반려동물 삭제]·2-3 삭제 확인 팝업·2-4 등록 카드 복귀(프론트 저장소에서 /start). API: `DELETE /pets/{petSn}`(주양육자만, 공동양육자 403 PET_PRIMARY_ONLY, data 없음).
- 화면 확인 필요(FN-MYP-002-08): 알림함 COM-004에 공동양육 연동 해제 알림(CO_PARENTING, 이동 경로 `/mypage/pets`)이 보이는지. 백엔드는 `PetService.java:273`에서 끝
- 팀 확인: 공동양육 강제 해제·자진 해제(`PetService.unlinkCaregiver`)에는 알림 발행이 없다. 기획이 이 경로의 알림까지 요구하는지 정하기
- JAJ에게 전달(SCR-MYP-002): `pet/domain/PetDeletedEvent(petSn)`를 `@TransactionalEventListener(phase = BEFORE_COMMIT)`(또는 `@EventListener`)로 받아 DAILY_HLTH_RCD·OBSRV_ARTCL·OBSRV_CYCL·SCHDL·CKUP_RCD(+하위)·REPT_PRVNT_MNG·CARE_ARTCL을 `DEL_YN='Y'`로 바꾸는 리스너, health 조회의 `CKUP_RCD.DEL_YN='N'` 조건. AFTER_COMMIT이면 실패해도 삭제가 커밋되므로 안 된다
- 연결 남음(SCR-MYP-005): FN-MYP-005-01·02 프론트 5-1 초대 모달, FN-MYP-005-03·04 프론트 SCR-MYP-002 하단 보낸 초대, FN-MYP-005-05·07·08·09·10 프론트 5-3 수락·5-4 거절 모달·5-5 이메일 불일치·5-6 만료·종료(프론트 저장소에서 /start). API: `POST·GET /caregiver-invitations`, `GET /caregiver-invitations/{jntBrupInvtnSn}`, `POST …/accept`(petSns)·`…/reject`. 초대 링크는 프론트 `/mypage/invitations/{jntBrupInvtnSn}`
- PR 본문에 "DB 변경 있음: `db/migration/20261030_01_add_del_yn_for_pet_delete.sql`(공용 DB `pawever` 2026-10-30 적용 완료)"를 적는다
- 연결 남음(SCR-MYP-004): FN-MYP-004-01·03·04 프론트 SCR-MYP-002 슬라이드 카드 가족 영역·[✕], FN-MYP-006-02·04 프론트 SCR-MYP-006 확인 팝업·토스트(프론트 저장소에서 /start). API: `GET /pets/{petSn}/caregivers`(`linkYmd` 추가), `DELETE /pets/{petSn}/caregivers/{brupRelSn}`
- LJS에게 알리기(SCR-MYP-004): `scheduler/CaregiverFake` → `PetService.searchCaregiverMbrSns(petSn)`, `expense/CaregiverPetFake` → `PetService.searchMyPets(mbrSn)`의 petSn·petNm으로 바꾸기
- 연결 남음: FN-MYP-030-01~06·09~21 프론트 SCR-MYP-030 가계부 기본 화면·전체 내역·편집 모드·한도 설정·삭제 모달·월 피커(프론트 저장소에서 /start). API: `GET /expenses/statistics`·`/expenses`·`/totals`·`/months`·`/pets`, `PATCH`·`DELETE /expenses/{expndSn}`, `DELETE /expenses?expndSn=`, `POST /expenses/bulk-delete`, `PUT /members/me/expense-limit`
- 연결 남음: FN-MYP-031-02·03·05·06 프론트 SCR-MYP-031 직접 입력·인식 결과 확인 저장·실패 후 직접 입력의 영수증 연결·중복 지출 확인 모달(프론트 저장소에서 /start)
- 화면 확인 필요(FN-MYP-030-08): 알림 목록 COM-004에 가계부 한도 알림이 보이는지. 백엔드는 `ExpenseService.java:313·316` 발행으로 끝
- LJS에게 알리기: `ReceiptService.markSavedByExpense`를 더함(인식 실패 후 직접 입력도 영수증을 저장완료로, LJS 파일)
- 배포 후 케어미션 출제·정산 배치가 느리면 개선: 지금은 한 행씩 INSERT·UPDATE하고 전체가 한 트랜잭션이다(로컬 시드 약 960마리로 30분, 2026-10-27 확인). 여러 행을 한 번에 넣고 묶음마다 커밋하도록 `/start`로 기획. 배치는 prod에서만 켜지므로 그 전에는 하지 않는다
- LJS에게 `MasterSeeder` 미션 목록 변경 알리기(21개로 바뀜, `CharacterSeeder`는 그대로)
- 연결 남음: 프론트 SCR-MYP-020 포인트 카드·요약·내역·케어미션 탭과 SCR-MYP-001 요약(프론트 저장소에서 /start, SCR-MYP-003은 체중 잠금은 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 판단, 기본 선택 동물은 `rprsPetYn`, 털색 드롭다운 설명), KSJ 펫이야기 글·댓글 → `reflectRepresentative`, LJS `PointFake` → `PointService.use`
- JAJ 확인: 케어미션 `reflect` 중 일정은 직접 입력(`DIRECT`)일 때만 반영한다(`CareScheduleService.java:87`). 명세와 같은지 확인
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
