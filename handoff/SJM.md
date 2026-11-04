# SJM 핸드오프

## 지금 하는 것
SCR-HLT-002 건강 일지 · SJM 몫(PetService.syncWeight) — 끝남 (승인 2026-11-04 09:25, 시작 2026-11-04 09:23). 검증 통과 1회차, 사람 확인 완료(`PetServiceTest`·`PetMapperXmlTest` 통과)

JAJ 요청. 체중 기록 등록·수정·삭제 뒤 프로필 체중을 맞추는 `PetService` 메서드를 더한다. 기획은 사람이 이 대화에서 승인했다(결정 1·2 포함). 기록 확인 뒤 `/impl`로 간다.

| 기능 | 바꿀 파일 | 완료 조건 |
| --- | --- | --- |
| FN-HLT-002-09~11 프로필 체중 동기화(화면은 SCR-HLT-002, JAJ) | `pet/mapper/PetMapper.java`, `mapper/pet/PetMapper.xml`, `pet/service/PetService.java`, `pet/service/PetServiceTest.java` | 명령: `./gradlew build -q` 통과(`ArchitectureTest` 포함). 화면: 없음(서버 내부 호출) |

- 결정 1: 시그니처는 `syncWeight(Long petSn, BigDecimal weight, Long actorMbrSn)`. `MDFR_ID = String.valueOf(actorMbrSn)`, `MDFCN_DT = NOW()`. 요청서의 인자 2개에서 3개로 바꾼 이유는 `MDFR_ID`로 누가 바꿨는지 추적하기 위해서다(`"SYSTEM"`은 배치 전용, `mapper.md`)
- 결정 2: `PET_BDWG`만 갱신한다. `MIN_BDWG`·`MAX_BDWG`·`GOAL_BDWG`는 건드리지 않는다
- Mapper: `int updatePetWeight(@Param("petSn") Long petSn, @Param("weight") BigDecimal weight, @Param("actorId") String actorId)`. `UPDATE PET SET PET_BDWG = #{weight}, MDFR_ID = #{actorId}, MDFCN_DT = NOW() WHERE PET_SN = #{petSn} AND DEL_YN = 'N'`
- Service: `@Transactional public void syncWeight(...)`. 영향 행이 0이면 `PET_NOT_FOUND`(404). 권한 검사는 하지 않는다(호출하는 쪽 담당). 호출하는 쪽 트랜잭션 안에서 실행한다. Javadoc에 둘 다 적는다
- 테스트(`PetServiceTest`, mapper mock): 정상이면 `petSn`·`weight`·`actorId`가 mapper에 그대로 넘어간다 / 영향 행 0이면 `PET_NOT_FOUND`. `PetMapperSqlTest`가 있으면 SQL에 `DEL_YN = 'N'`과 `PET_BDWG`만 갱신하는지 검사를 더한다(없으면 만들지 않는다)
- 층 규칙: `health`(2층)가 `pet`(1층) service를 부른다. 허용된다
- 이번엔 안 함: JAJ 코드 `health/service/PetWeightFake`, `DailyHealthRecordService`와 그 테스트는 이 세션에서 고치지 않는다
- JAJ 전환 방법(구현 뒤 "다음에 할 것"에 남긴다): `PetWeightFake`를 지우고 `PetService`를 주입한다. `DailyHealthRecordService.syncWeight`의 `petWeightFake.sync(petSn, latest)`를 `petService.syncWeight(petSn, latest, mbrSn)`로 바꾸고, 이를 위해 `syncWeight`에 `mbrSn` 인자를 더한다(`create`·`update`·`delete`가 넘긴다). `DailyHealthRecordServiceTest`의 mock도 `PetService`로 바꾼다. 시그니처가 요청서와 달리 인자 3개인 이유는 `MDFR_ID` 추적이다
- 프론트 연결: 없음

새 의존성: 없음

## 다음에 할 것
- 연결 남음(SCR-HLT-002 FN-HLT-002-09~11): JAJ가 `health/service/PetWeightFake`를 `PetService.syncWeight(petSn, weight, actorMbrSn)`로 바꾸기. `PetWeightFake`를 지우고 `PetService`를 주입, `DailyHealthRecordService.syncWeight`에 `mbrSn`을 더해 `petService.syncWeight(petSn, latest, mbrSn)`를 부르고(`create`·`update`·`delete`가 `mbrSn`을 넘긴다), `DailyHealthRecordServiceTest`의 mock도 `PetService`로 바꾼다. 시그니처가 요청서와 달리 인자 3개인 이유는 `MDFR_ID` 추적이다. 권한 검사는 호출하는 쪽, 같은 트랜잭션 안에서 부른다. 삭제됐거나 없는 반려동물이면 404 `PET_NOT_FOUND`로 호출한 쪽도 롤백된다
- 연결 남음(SCR-MYP-030, FN-MYP-030-18·19·21): 프론트 SCR-MYP-030 편집 모드에서 전체 선택 뒤 해제한 번호를 `excludeExpndSn`으로 보내고 `expectedCnt`를 "조건 건수 − 해제한 수"로 바꾸기. 409 `EXPENSE_BULK_DELETE_CHANGED`면 목록을 다시 불러오게 안내(프론트 저장소에서 /start)
- 연결 남음(SCR-MYP-002 목록 응답 보강): 프론트 SCR-MYP-002 슬라이드의 "(중성화 완료)"·"정상 a~bkg"를 상세 조회 대신 `GET /pets`의 `nutrYn`·`minBdwg`·`maxBdwg`로 채우기(프론트 저장소에서 /start)
- Postman 컬렉션: 3번 회원이 정지(`MEMBER_INACTIVE`)라 그 회원을 쓰는 변수(`mbrSnB`·`emailB`·`delCoMbrSn`·`delCoEmail`·`invReceiverMbrSn`·`infoOtherMbrSn`)가 있는 폴더는 403. 돌릴 때 정상 회원으로 바꾼다(이메일은 `GET /members/me`의 `acntEml`)
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
