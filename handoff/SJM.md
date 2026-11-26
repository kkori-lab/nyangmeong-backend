# SJM 핸드오프

## 지금 하는 것
SCR-MYP-003 강아지 털색 검증에서 얼룩(PETC0011) 빼기 (FN-MYP-003-03) — 끝 (승인 2026-11-26 11:34, 시작 2026-11-26, 검증 통과·사람 확인 2026-11-26)
- 바꿀 파일: `pet/domain/PetColor.java` — `RETIRED`(PETC0011) 집합을 두고 `fits()`가 종과 관계없이 거절. 상수는 `DOG_ONLY`에서 옮겨 남기고 지우지 않음. 조회·표시는 `fits()`를 안 거쳐 그대로
- 바꿀 파일: `src/test/.../PetServiceTest.java` — 강아지·고양이 등록, 수정에서 PETC0011 거절, 다른 털색 통과 테스트. 기존 `DOG_ONLY_COLOR` 상수는 PETC0010(블랙탄)으로 바꿈
- 거절 응답은 지금과 같은 `INVALID_INPUT_VALUE`(필드 `colrCd`). DB·공통코드는 안 건드림
- 완료 조건: 명령: `./gradlew build -q` 종료 코드 0. 화면: PETC0011로 강아지·고양이 등록·수정 400, 기존 PETC0011 반려동물 조회 이름 "얼룩", 다른 털색 등록·수정 정상
- 주의: `updatePet`도 `checkPet`을 거치므로 PETC0011로 저장된 반려동물(3건)은 털색을 바꾸지 않고 다른 항목만 고쳐도 400이 난다. 털색을 다른 색으로 바꾸면 통과
새 의존성: 없음
- 사람 결정 2026-11-26: API 직접 확인 생략(단위 테스트로 대체). `PetServiceTest`가 등록·수정 거절과 다른 털색 통과를 덮고, 공통코드 PETC0011이 이미 `USE_YN='N'`이라 위험이 낮다. 검증 에이전트 권한은 주지 않음. 검증 1회차 `실패`는 이 확인 못 함 때문이다(빌드 종료 코드 0, 코드 결함 없음)

## 다음에 할 것
- 관리자 케어미션 수정(FN-ADM-071-02): `cmpnPnt`는 출제 배점에 안 쓰이게 된다. 수정 화면·API에서 뺄지 정한다. 관리자 화면(CareMissionTable)에 아직 표시·수정됨, LJS 몫
- 받은 초대 목록 API를 만들 때: 취소·삭제된 초대는 넣지 않는다(대기 중만)
- 로컬 설정(SJM): `application.properties` permit-all-paths의 `/api/files/public/**`는 PR #234 병합 뒤 develop을 합칠 때 다시 지운다(git에 올라가지 않음)
- (남음: 건강기록 syncWeight가 아직 범위를 갱신하지 않음) JAJ에게 전달(건강기록 체중 범위 갱신, FN-MYP-003-04): 정상 범위 계산은 `com.nyangmeong.pet.service.PetBdwgRange`(public record `min`·`max`)에 있다. 사용: `PetBdwgRange r = PetBdwgRange.of(기준체중);` 후 `r.min()`·`r.max()`를 `PET.MIN_BDWG`·`MAX_BDWG`에 저장한다(다른 도메인이므로 `pet/service` 클래스를 바로 쓴다). 규칙: 최소 = 기준 체중 × 0.9, 최대 = × 1.1, 둘 다 소수 둘째 자리 HALF_UP(4.25 → 3.83 ~ 4.68). 아주 작은 체중은 최소를 0.01 이상, 최대를 최소+0.01 이상으로 보정한다(`CK_PET_WGHT_RANGE` 유지). 체중이 null이거나 0 이하면 `IllegalArgumentException`. 최대는 200kg(`PetBdwgRange.MAX_BDWG_LIMIT`)에서 자른다. 최소가 200 이상이면 199.99로 자른다. 갱신 전에 `PET.BDWG_RNG_DRCT_INPT_YN`을 확인해 `'Y'`(사용자 직접 입력)이면 범위를 바꾸지 않는다. 마이페이지(등록·수정)는 이미 이 메서드를 쓴다
- 팀에 알린다(시드): `db/seed/20261028_seed_data.sql`의 PET·체중 기록(HLTC0014)을 정리했고(품종 직접 입력명, 품종별 털색, 얼룩 미사용, 체중 소수 첫째 자리) `db/20261109_insert_base_data_v0.14.sql` 변경 절 v0.14 (8)로 얼룩(PETC0011)을 사용 중지했다. 공용 DB는 2026-11-10 반영 완료. 이미 시드를 넣은 로컬 DB는 시드를 다시 넣지 말고 팀에서 받은 정리 SQL로 맞춘다
- 확인: 등록 직후 출제(`PetCreatedEventListener`)가 UK_DAY_MSN_1 등으로 실패하면 `issueFor`가 리스너의 REQUIRES_NEW 트랜잭션에 합류해 롤백 전용이 되어, 커밋 때 `UnexpectedRollbackException` 로그가 함께 찍힐 수 있다(등록 응답에는 영향 없음, 다음 정각 배치가 채움). 거슬리면 `issueFor`를 `REQUIRES_NEW`로 바꾸는 것을 고려
- Postman 컬렉션: 3번 회원이 정지(`MEMBER_INACTIVE`)라 그 회원을 쓰는 변수(`mbrSnB`·`emailB`·`delCoMbrSn`·`delCoEmail`·`invReceiverMbrSn`·`infoOtherMbrSn`)가 있는 폴더는 403. 돌릴 때 정상 회원으로 바꾼다(이메일은 `GET /members/me`의 `acntEml`)
- YAY와 정하기(SCR-MYP-040): POST /members/me/password-verification은 서버에 상태를 남기지 않게 만들어졌고, PATCH /members/me는 본인 확인 통과를 검사하지 않는다. 이 방식으로 확정할지 정한다
- 화면 확인 필요(FN-MYP-002-08): 알림함 COM-004에 공동양육 연동 해제 알림(CO_PARENTING, 이동 경로 `/mypage/pets`)이 보이는지. 백엔드는 `PetService.java:273`에서 끝
- 화면 확인 필요(FN-MYP-030-08): 알림 목록 COM-004에 가계부 한도 알림이 보이는지. 백엔드는 `ExpenseService.java:313·316` 발행으로 끝
- 배포 후 케어미션 출제·정산 배치가 느리면 개선: 지금은 한 행씩 INSERT·UPDATE하고 전체가 한 트랜잭션이다(로컬 시드 약 960마리로 30분, 2026-10-27 확인). 여러 행을 한 번에 넣고 묶음마다 커밋하도록 `/start`로 기획. 배치는 prod에서만 켜지므로 그 전에는 하지 않는다
- 확인 못 함(화면): 공용 DB에 파기된 회원이 양육자인 반려동물이 없어 `GET /api/pets/{petSn}`의 「탈퇴한 회원」 표시를 실제 응답으로 보지 못함. 테스트와 커뮤니티의 같은 조각으로 판정
