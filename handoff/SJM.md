# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 추가-3 `PATCH /pets/{petSn}` 목표체중 비우기(`goalBdwgReset`) — `./gradlew build -q` 통과(PetServiceTest 3건 추가), 사람 확인 통과(Postman Runner 16건, mbrSn 6·petSn 20, 되돌리기 포함), 프론트 화면 확인 통과. PR #86(병합 전). 앞선 SCR-MYP-005 추가-1·2는 끝남(#74·#84 병합)

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `db/migration/20261030_01_add_del_yn_for_pet_delete.sql`(새 파일), `pet/domain/PetDeletedEvent`(새 파일), `PetService.deletePet`, `CareMissionMapper`·`PixelCharacterMapper`·`CaregiverInvitationMapper`(Java·XML), `docs/convention/service.md`, `docs/api/issues.md` 마이페이지 3번, 관련 테스트 | 명령: `./gradlew build -q`(PetServiceTest에 삭제 시 일별 미션·캐릭터 생성 작업·캐릭터 사진·초대 대상 DEL_YN 처리와 `PetDeletedEvent` 발행 확인). 화면: 마이그레이션 실행 뒤 주양육자 토큰 `DELETE /api/pets/{petSn}` → 200, DB에서 그 반려동물의 DAY_MSN·CHAR_CRT_JOB·CHAR_PHOTO·INVTN_TRGT_PET이 `DEL_YN='Y'`, EXPND는 그대로 | 요청 2026-10-30 05:54 "반려동물 삭제 시 가계부만 남기고 나머지는 한 트랜잭션에서 soft delete. DEL_YN 없는 테이블(CKUP_RCD·DAY_MSN·CHAR_CRT_JOB·CHAR_PHOTO·INVTN_TRGT_PET)에 컬럼 추가는 migration으로, pet 쪽 이벤트 발행까지. health·ai 리스너는 JAJ·LJS" |
| 추가-2 | `pet/dto/CaregiverInvitationConflictResponse`(→ linkedPets·pendingPets 목록), 새 `CaregiverInvitationLinkedPet`·`CaregiverInvitationPendingPet`, `CaregiverInvitationService.createInvitation`, `CaregiverInvitationServiceTest`·`CaregiverInvitationControllerTest`, `docs/api/pet.md` | 명령: `./gradlew build -q`(여러 마리 충돌을 모두 모음, 섞이면 CAREGIVER_ALREADY_LINKED, 충돌 시 초대 미생성, 빈 목록은 []). 화면: 409 `data`가 `{linkedPets:[{petSn,petNm}], pendingPets:[{petSn,petNm,invtnExpryDt}]}` | 요청 2026-10-31 10:42 "POST /caregiver-invitations 409가 첫 충돌 한 마리만 줌. 선택한 동물을 전부 검사해 이미 가족·대기 초대를 목록으로 모아 주기, code는 기존 두 개, 충돌 있으면 초대 안 만듦, 다른 동작은 건드리지 않기" |
| 추가-3 | `pet/dto/PetUpdateRequest`(`goalBdwgReset` 추가), `PetService.updatePet`, `PetServiceTest`, `docs/api/pet.md`, `docs/api/issues.md` 마이페이지 4번 | 명령: `./gradlew build -q`(reset=true면 GOAL_BDWG null 저장, reset과 goalBdwg 함께면 400 BAD_REQUEST·저장 안 함, 둘 다 없으면 그대로). 화면: `PATCH /api/pets/{petSn}` `{"goalBdwgReset":true}` → 200, 응답·DB GOAL_BDWG null | 요청 2026-10-31 "PATCH /pets/{petSn}에서 목표체중을 비울 수 있게. goalBdwgReset(Boolean), true면 null 저장, goalBdwg와 같이 보내면 400(profilePhotoReset과 같은 방식), 둘 다 없으면 변경 안 함, 다른 동작은 건드리지 않기" |

## 다음에 할 것
- 사람 확인(SCR-MYP-003 추가-3): 통과 2026-10-31. Postman Runner 16건 통과·실패 0(mbrSn 6, petSn 20). ① goalBdwg 4.5 200 ② goalBdwg+reset 400 BAD_REQUEST ③ 4.5 그대로 ④ reset=false 4.5 그대로 ⑤ reset=true null, 원래 값으로 되돌림
- 프론트 연결(SCR-MYP-003 추가-3): 완료(프론트 PR에 포함). `PetFormModal`이 목표체중 지우기를 `goalBdwgReset: true`로 보낸다. 로컬 백엔드로 화면 확인함(지우고 저장 → 다시 열면 비어 있음). 품종은 `vrtySn`·`vrtyDrctInptNm` 중 하나만 보내면 다른 쪽이 비워진다
- 연결 남음(SCR-MYP-005 추가-2): 프론트 5-1 초대 모달의 409 처리를 `data.linkedPets`("이미 ○○의 가족이에요")·`data.pendingPets`("이미 초대를 보냈어요 (MM-DD에 만료돼요)") 목록 기준으로 바꾸기(프론트 저장소에서 /start). 예전 단일 `data.petSn·petNm·invtnExpryDt`는 없어졌다
- 연결 남음(SCR-MYP-040): FN-MYP-040-02·03·08·09·13 프론트 SCR-MYP-040 6-2·6-3 프로필 요약·계정 정보, FN-MYP-040-02·05·08·11 프론트 6-2·6-3 사진 변경·기본 이미지로·휴대폰 번호 [수정]·[저장](프론트 저장소에서 /start). API: `GET /members/me`(`MemberResponse`에 `joinDt`·`lastLgnDt` 추가), `PATCH /members/me`(multipart `mblTelno`·`profilePhoto`·`profilePhotoReset`, 다른 회원 번호 409 PHONE_ALREADY_USED, 사진과 되돌리기 함께 400)
- YAY와 정하기(SCR-MYP-040): 본인 확인(6-1)을 통과했는지 `PATCH /members/me`에서 서버가 검사하지 않는다. `POST /members/me/password-verification`을 만들 때 통과 표시를 어떻게 남길지 함께 정한다
- 연결 남음(SCR-MYP-002): FN-MYP-002-07 프론트 SCR-MYP-002 ⋮ 메뉴 [반려동물 삭제]·2-3 삭제 확인 팝업·2-4 등록 카드 복귀(프론트 저장소에서 /start). API: `DELETE /pets/{petSn}`(주양육자만, 공동양육자 403 PET_PRIMARY_ONLY, data 없음). FN-MYP-002-08 LJS 알림함 COM-004에 공동양육 연동 해제 알림(CO_PARENTING, 이동 경로 `/mypage/pets`) 표시
- JAJ에게 전달(SCR-MYP-002): `pet/domain/PetDeletedEvent(petSn)`를 `@TransactionalEventListener(phase = BEFORE_COMMIT)`(또는 `@EventListener`)로 받아 DAILY_HLTH_RCD·OBSRV_ARTCL·OBSRV_CYCL·SCHDL·CKUP_RCD(+하위)·REPT_PRVNT_MNG·CARE_ARTCL을 `DEL_YN='Y'`로 바꾸는 리스너, health 조회의 `CKUP_RCD.DEL_YN='N'` 조건. AFTER_COMMIT이면 실패해도 삭제가 커밋되므로 안 된다
- 연결 남음(SCR-MYP-005): FN-MYP-005-01·02 프론트 5-1 초대 모달, FN-MYP-005-03·04 프론트 SCR-MYP-002 하단 보낸 초대, FN-MYP-005-05·07·08·09·10 프론트 5-3 수락·5-4 거절 모달·5-5 이메일 불일치·5-6 만료·종료(프론트 저장소에서 /start). API: `POST·GET /caregiver-invitations`, `GET /caregiver-invitations/{jntBrupInvtnSn}`, `POST …/accept`(petSns)·`…/reject`. 초대 링크는 프론트 `/mypage/invitations/{jntBrupInvtnSn}`
- PR 본문에 "DB 변경 있음: `db/migration/20261030_01_add_del_yn_for_pet_delete.sql`(공용 DB `pawever` 2026-10-30 적용 완료)"를 적는다
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
