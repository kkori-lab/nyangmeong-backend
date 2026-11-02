# SJM 핸드오프

## 지금 하는 것
SCR-MYP-002 반려동물 프로필 목록 · 목록 응답에 중성화 여부·정상 체중 범위 추가 — 기획 (시작 2026-11-02 10:38)

근거: 마이페이지_기능명세서_v0.5_20261101 FN-MYP-002-02 "나이 · 품종 · 성별(중성화)", "체중(정상범위 함께 표시)". 마이페이지_화면설계서_v0.6_20261025 SCR-MYP-002 같은 문구. 명세에 새 기능 ID는 없다.

- FN-MYP-002-02 반려동물 슬라이드(목록 응답 보강): `pet/dto/PetSummary`에 `nutrYn`(boolean, `PetResponse`와 같게)·`minBdwg`·`maxBdwg`(BigDecimal) 추가, `mapper/pet/PetMapper.xml` `petSummaryResultMap`·`selectList`에 `P.NUTR_YN`(`RPRS_PET_YN`처럼 'Y'→TRUE, 그 밖→FALSE)·`P.MIN_BDWG`·`P.MAX_BDWG` 추가, `docs/api/pet.md` PetSummary 표에 세 필드 추가(근거 PET.NUTR_YN·MIN_BDWG·MAX_BDWG, MYP-002-02, 세 컬럼 모두 NOT NULL이라 필수 Y), `PetServiceTest`·`PetControllerTest` 목록 테스트에 세 필드 확인 추가. API: `GET /pets`(SJM, 응답 `List<PetSummary>`, 기존 필드는 그대로). 완료 조건 — 명령: `./gradlew build -q`(PetControllerTest가 `GET /api/pets` 응답 JSON에 `nutrYn`·`minBdwg`·`maxBdwg`를 확인). 화면: Postman `GET /api/pets` → 200, 각 항목에 `nutrYn`(true/false)·`minBdwg`·`maxBdwg`가 있고 값이 같은 동물의 `GET /api/pets/{petSn}`과 같다. 공동양육 동물에도 나온다. 프론트 연결: SCR-MYP-002 슬라이드의 "(중성화 완료)"·"정상 a~bkg"를 상세 조회 대신 목록 값으로 채우기 (프론트 저장소에서 /start)
- 다른 쪽 영향: `PetSummary`를 쓰는 `ai/AiRegisterContextService`·`expense/ExpenseQueryService`·`CaregiverInvitationService`는 필드 추가만이라 고치지 않는다(컴파일·기존 테스트로 확인)

새 의존성: 없음

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
