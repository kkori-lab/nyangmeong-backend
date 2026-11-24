# SJM 핸드오프

## 지금 하는 것
SCR-MYP-002 파기 회원의 반려동물 일괄 삭제·공동양육 연동 해제 — 끝남 (승인 2026-11-24 14:47, 시작 2026-11-24 15:05). 검증 1회차 판정 `통과`, 사람 확인 완료
- `PetService.purgeMemberPets(mbrSn)` 추가(주양육 연동은 `deletePetCascade`로 삭제, 공동양육 연동은 해제 + PETC0036 이력, 주양육자 알림 없음). `deletePet` 본문은 `deletePetCascade`로 분리(동작 그대로). `PetMapper.selectLinkedRelsByMember`, `LinkChangeType.MEMBER_PURGED`
- 공용 DB 쓰기: `db/20261120_insert_base_data_v0.15.sql` 변경 절 v0.15 (1) PETC0036 실행 완료. 새 의존성 없음

## 다음에 할 것
- 파기 배치 연결(YAY): MemberPurgeService.purge가 같은 트랜잭션에서 member 도메인의 MemberPurgedEvent(mbrSn)을 발행하고, pet이 @EventListener로 받아 purgeMemberPets를 호출하는 방식 제안(스케줄러로 옮기면 회원 파기와 반려동물 삭제가 다른 트랜잭션이 됨). 이벤트가 생기면 리스너는 SJM이 붙임. member(0층)는 pet(1층)을 직접 못 부름
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
