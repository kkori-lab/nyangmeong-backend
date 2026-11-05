# SJM 핸드오프

## 지금 하는 것
핸드오프 정리 · "프론트 저장소에서 /start" 연결 남음 줄 대조 — 끝남 (승인 2026-11-06 00:45, 시작 2026-11-06 00:43). 검증 통과 1회차, 사람 확인 완료

코드는 바꾸지 않는다. 이 파일 `## 다음에 할 것`만 고친다. 프론트 `milestones/SJM.md`의 해당 절이 모두 `끝`이고, 프론트 코드에서 API·필드 사용을 확인했다.

| 줄 | 프론트 근거 | 처리 |
| --- | --- | --- |
| SCR-MYP-030 `excludeExpndSn`·409 | `expense/api.js`·`ExpenseBulkDeleteDialog.jsx`, `queries.js`의 `EXPENSE_BULK_DELETE_CHANGED`, 절 "전체 선택 뒤 해제한 건을 제외하고 삭제 — 끝" | 지움 |
| SCR-MYP-002 목록 `nutrYn`·`minBdwg`·`maxBdwg` | `PetSlide.jsx`, 절 "슬라이드 중성화·정상 체중을 목록 값으로 — 끝" | 지움 |
| SCR-MYP-005 추가-2 `linkedPets`·`pendingPets` | `InviteModal.jsx` | 지움 |
| SCR-MYP-040 프로필·계정·사진·휴대폰 | `member/api.js`(`joinDt`·`profilePhotoReset`), `AccountInfoCard.jsx`, `PhoneField.jsx`(`PHONE_ALREADY_USED`), 절 "조회·휴대폰 번호·프로필 사진 API 연결 — 끝" | 지움 |
| SCR-MYP-002 반려동물 삭제 | `pet/api.js`의 `deletePet`, 절 "반려동물 삭제를 실제 API로 — 끝" | 지움 |
| SCR-MYP-005 초대 모달·수락·거절 | `pet/api.js`의 `/caregiver-invitations`, 절 "초대 가짜 응답을 실제 API로 — 끝" | 지움 |
| SCR-MYP-004·006 가족 영역·연동 해제 | `FamilySection.jsx`(`linkYmd`), 절 "가족 영역…실제 API로"·"확인 팝업·토스트를 실제 API로 — 끝" | 지움 |
| SCR-MYP-030 가계부 기본 화면 등 | `expense/api.js`(`/bulk-delete`·`expense-limit`), `ExpenseLimitModal.jsx`, 절 "가계부 가짜 응답을 실제 API로 — 끝" | 지움 |
| SCR-MYP-031 직접 입력·영수증 연결·중복 모달 | `ExpenseAddModal.jsx`·`ExpenseEditModal.jsx`의 중복 처리, 절 SCR-MYP-031 둘 다 `끝` | 지움 |
| SCR-MYP-020·001·003 포인트·요약·체중 잠금 | `point/api.js`, `PetMiniCard.jsx`(`rprsPetYn`), `pet/api.js`(`HLTC0014`), 절 SCR-MYP-020·001·003 체중 잠금 `끝` | 프론트 부분 지움. 같은 줄의 백엔드 남의 몫은 새 줄로 남김(아래) |

- 남기는 줄(새로 씀): "KSJ·LJS에게 알리기: KSJ 펫이야기 글·댓글 → `PetService.reflectRepresentative`(아직 `community`에 호출 없음), LJS `pet/service/PointFake` → `PointService.use`(아직 `ItemService`가 `PointFake`를 씀)". 둘 다 백엔드라 프론트 대조와 무관하게 아직 안 됐다
- 맨 위 "정리(이 파일)" 줄도 지운다
- 그 밖의 줄(Postman, YAY·JAJ·LJS·팀 확인, 화면 확인 필요, PR 본문, 배치 개선 등)은 프론트 연결 줄이 아니라 건드리지 않는다
- 완료 조건: 명령: `sed -n '/^## 다음에 할 것/,$p' handoff/SJM.md | grep -c "프론트 저장소에서 /start"`가 0. `./gradlew build -q` 통과(코드 변경 없음 확인용). 화면: 없음

새 의존성: 없음

## 다음에 할 것
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
