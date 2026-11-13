# SJM 핸드오프

## 지금 하는 것
마이페이지 · 반려동물 체중 정상범위 자동 계산(SCR-MYP-003, FN-MYP-003-04) — 끝남 (승인 2026-11-13 10:38). 검증 통과 2회차(1회차는 공용 DB에 컬럼이 없어 화면 확인 불가로 실패), 사람 확인 완료(Postman 42/42 통과, 시험 데이터 정리함)

마이페이지(반려동물 등록·수정)만. 건강일지 체중 기록 때 범위 갱신과 그래프는 건강기록 담당(JAJ) 몫이라 건드리지 않는다. 정책: 정상범위 = 기준 체중 × 0.9 ~ × 1.1, 소수 둘째 자리 HALF_UP. `PET.BDWG_RNG_DRCT_INPT_YN` 'Y' = 사용자 직접 입력, 'N' = 자동 계산, 'Y'는 자동으로 바꾸지 않는다.

- `db/20261109_create_tables_v0.14.sql`: PET에 `BDWG_RNG_DRCT_INPT_YN char(1) NOT NULL DEFAULT 'Y'`(MAX_BDWG 다음)와 `CK_PET_BDWG_RNG_DRCT_INPT_YN` 추가, 맨 위 변경 절에 v0.14 (12) ALTER 주석 추가(기존 행은 DEFAULT로 'Y'). 새 파일은 만들지 않는다(shared-files.md). DB에는 실행하지 않는다. 시드 파일은 기본값으로 충분해 고치지 않는다
- `pet/service/PetBdwgRange.java`(새): `public static PetBdwgRange of(BigDecimal 기준체중)` → min·max record. ×0.9·×1.1을 소수 둘째 자리 HALF_UP, 최소는 0.01 이상, 최대는 최소+0.01 이상으로 보정. 최대 체중 상한은 상수 자리만 두고 적용하지 않는다(200kg 상한은 회의 확정 후)
- `Pet`·`PetMapper.xml`·`PetMapper.java`: 컬럼 매핑(insert·update·select·petResultMap·petSummaryResultMap)
- `PetCreateRequest`: `bdwgRngDrctInptYn`(@Pattern [YN], 안 보내면 'Y'), `minBdwg`·`maxBdwg`의 @NotNull 제거(서비스에서 'Y'일 때만 필수). `PetUpdateRequest`: `bdwgRngDrctInptYn` 추가. `PetResponse`·`PetSummary`: 내려줌
- `PetService`: 등록 'N'이면 petBdwg로 계산해 저장(보낸 값 무시), 'Y'면 보낸 값 그대로(둘 다 필수). 수정: 'N'으로 바꾸면 현재(같이 보낸 petBdwg가 있으면 그 값) 체중으로 재계산, 'Y'이면서 범위를 보내면 그대로 저장·안 보내면 기존 값 유지, 플래그 없이 petBdwg만 보내면 현재 'N'일 때 재계산·'Y'면 유지, 현재 'N'인데 플래그 없이 minBdwg·maxBdwg를 보내면 'Y'로 바꾸고 보낸 값 저장
- `docs/api/pet.md`: 등록·수정·응답 표에 필드와 규칙 추가
- `handoff/SJM.md`: 건강기록 담당이 `PetBdwgRange.of()`를 쓰는 위치·사용법·반올림 규칙
- 테스트: `PetBdwgRangeTest`(새, 경계·보정), `PetServiceTest`(등록·수정 분기), `PetControllerTest`(검증)
- 완료 조건:
  - 명령: `./gradlew build -q` 통과
  - 화면: Postman으로 POST /pets 'N'이면 서버 계산 범위 저장, 'Y'면 보낸 값 저장, 플래그 생략이면 'Y', PATCH /pets/{petSn}에서 'N' 전환·체중만 변경·범위만 변경 동작, GET /pets·/pets/{petSn}에 bdwgRngDrctInptYn 노출(DB에 컬럼 추가 후)
- DB 변경 있음: 변경 절 v0.14 (12) (공용 DB 실행함 2026-11-13)
- 프론트 연결: 마이페이지 반려동물 등록·수정 모달의 bdwgRngDrctInptYn 전송과 표시 (프론트 저장소에서 /start)
새 의존성: 없음

## 다음에 할 것
- 프론트 연결(SCR-MYP-003, FN-MYP-003-04): 등록·수정 모달이 `bdwgRngDrctInptYn`을 보내고 보여 주기(프론트 저장소에서 `/start`, 프론트 마일스톤에 대기 절을 넣어 뒀다)
- JAJ에게 전달(건강기록 체중 범위 갱신, FN-MYP-003-04): 정상 범위 계산은 `com.nyangmeong.pet.service.PetBdwgRange`(public record `min`·`max`)에 있다. 사용: `PetBdwgRange r = PetBdwgRange.of(기준체중);` 후 `r.min()`·`r.max()`를 `PET.MIN_BDWG`·`MAX_BDWG`에 저장한다(다른 도메인이므로 `pet/service` 클래스를 바로 쓴다). 규칙: 최소 = 기준 체중 × 0.9, 최대 = × 1.1, 둘 다 소수 둘째 자리 HALF_UP(4.25 → 3.83 ~ 4.68). 아주 작은 체중은 최소를 0.01 이상, 최대를 최소+0.01 이상으로 보정한다(`CK_PET_WGHT_RANGE` 유지). 체중이 null이거나 0 이하면 `IllegalArgumentException`. 최대 체중 상한은 `PetBdwgRange.MAX_BDWG_LIMIT` 자리만 있고(null) 아직 적용하지 않는다(200kg 상한은 회의 확정 후). 갱신 전에 `PET.BDWG_RNG_DRCT_INPT_YN`을 확인해 `'Y'`(사용자 직접 입력)이면 범위를 바꾸지 않는다. 마이페이지(등록·수정)는 이미 이 메서드를 쓴다
- DB 공용 반영(SCR-MYP-003): `db/20261109_create_tables_v0.14.sql` 변경 절 v0.14 (12)는 공용 DB에 반영 완료(2026-11-13, 사람이 실행). 병합 뒤 다시 실행하지 않는다(중복 컬럼 오류). PR 본문에 "DB 변경 있음: v0.14 (12)"를 적고 완료 댓글을 남긴다. 기존 PET 행은 DEFAULT로 모두 'Y'
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
