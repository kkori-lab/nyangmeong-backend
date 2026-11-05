# SJM 마일스톤

기준: 마이페이지\_기능명세서\_v0.4_20261025 (DMS, 2026-10-26 확인)

## SCR-MYP-001 마이페이지 홈 (반려동물 목록) — 끝

- 기능: FN-MYP-001-01 내 반려동물 카드 조회(캐릭터 얼굴 이미지는 LJS), FN-MYP-002-02 반려동물 슬라이드(화면은 SCR-MYP-002), FN-MYP-002-03 인식칩 번호 표시(화면은 SCR-MYP-002)
- 함께 만드는 것: `pet` 도메인의 PET·BRUP_REL 조회 Mapper, 다른 도메인이 부를 내 반려동물 목록 service 메서드
- 기다리는 것: 없음
- 완료 조건: `GET /api/pets`가 내가 주양육·공동양육하는 반려동물만 역할·대표 여부·공동양육 인원과 함께 돌려준다. 삭제된 반려동물과 연동 해제된 관계는 빠진다

## SCR-MYP-003 반려동물 등록/수정 — 끝

- 기능: FN-MYP-003-01 반려동물 등록/수정, FN-MYP-003-02 품종 선택, FN-MYP-003-03 털색 선택, FN-MYP-003-04 목표체중·체중 정상범위, FN-MYP-003-05 인식칩 번호 등록, FN-MYP-003-06 중성화 여부 수정, FN-MYP-003-07 알레르기·기저질환 입력, FN-MYP-002-01 등록 상한 10마리(화면은 SCR-MYP-002), FN-MYP-002-04 수정·프로필 조회(화면은 SCR-MYP-002), FN-MYP-010-05 대표 반려동물 지정(화면은 SCR-MYP-010)
- API가 남의 것: FN-MYP-003-02 품종 목록 `GET /breeds` (KSJ), FN-MYP-003-03 털색 공통코드 `GET /common-codes` (LJS)
- 함께 만드는 것: 프로필 체중 동기화 service 메서드(건강 일지 체중 기록이 부름, JAJ), 첫 등록 시 대표 자동 지정
- 기다리는 것: 품종 마스터 (KSJ)
- 완료 조건: 11번째 등록은 막힌다. 첫 등록 반려동물이 대표가 되고, 대표 지정 API로 주양육 동물만 대표로 바꿀 수 있다

## SCR-MYP-020 포인트 관리/케어미션 — 끝

- 기능: FN-MYP-020-01 보유 포인트, FN-MYP-020-02 적립·사용 내역, FN-MYP-020-03 케어미션 동물 탭, FN-MYP-020-04 케어미션 기록·날짜 이동, FN-MYP-020-05 미션 포인트 지급 규칙, FN-MYP-020-06 공동양육자 수행분 처리, FN-MYP-020-08 검진기록 등록 포인트, FN-MYP-020-09 포인트 요약, FN-MYP-001-03 포인트·지출 한눈에 보기(화면은 SCR-MYP-001, 지출액은 SCR-MYP-030의 통계 API), FN-MYP-001-04 오늘의 케어미션 요약(화면은 SCR-MYP-001)
- 함께 만드는 것: 포인트 적립·차감 service 메서드(아이템 구매 LJS, 검진 포인트 JAJ), 케어미션 반영 service 메서드(건강기록 JAJ, 커뮤니티 KSJ, 메인 YAY), 자정 정산 배치
- 기다리는 것: 검진기록 등록·영수증 빠른 등록에서 검진 포인트 호출 (JAJ, FN-MYP-020-08)
- 완료 조건: 포인트가 부족하면 차감이 `POINT_NOT_ENOUGH`로 막힌다. 같은 반려동물·검진일로 검진 포인트를 두 번 요청하면 한 번만 적립된다
- 연결 남음: FN-MYP-020-01·09, FN-MYP-001-03·04 (프론트 연결 SCR-MYP-020 포인트 카드·요약, SCR-MYP-001 요약, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-020-02 (프론트 연결 SCR-MYP-020 포인트 내역, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-020-03·04·07 (프론트 연결 SCR-MYP-020 케어미션 탭·날짜 이동·0마리 안내, SCR-MYP-001 오늘의 케어미션, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-020-08 (검진기록 등록·영수증 빠른 등록에서 `earnCheckupPoint` 호출, JAJ) → `CheckupService.java:283`에서 끝 (영수증 등록도 같은 insert를 지나 호출, 사람 확인)
- 연결 남음: FN-MYP-020-04·06 (건강 일지 기록·케어리스트 기본 항목 체크·일정·관찰 기록 → `reflect` JAJ, 관찰 중 반려동물 조회 JAJ) → `DailyHealthRecordService.java:89`, `CareItemService.java:492`, `CareScheduleService.java:88`, `ObservationService.java:142·181`의 `reflect`와 `scheduler/CareMissionJob.java:53`의 `searchObservingPetSns()`에서 끝
- 연결 남음: FN-MYP-020-04·06 (펫이야기 글·댓글 → `reflectRepresentative` KSJ, 아이템 구매 `PointFake` → `PointService.use` LJS)

## SCR-MYP-031 가계부 등록/수정 — 끝

- 기능: FN-MYP-031-02 직접 입력 폼, FN-MYP-031-03 인식 결과 확인·수정, FN-MYP-031-05 영수증 업로드(인식 실패 후 직접 입력 저장의 영수증 연결), FN-MYP-031-06 중복 지출 확인, FN-MYP-030-08 한도 알림(화면은 SCR-MYP-030)
- API가 남의 것: FN-MYP-031-03·05 영수증 업로드·분석 (LJS, `expense` 영수증 API 재사용)
- 함께 만드는 것: 지출 생성·중복 확인 service 메서드(영수증 저장 LJS가 부름)
- 기다리는 것: 없음
- 완료 조건: `POST /api/expenses`가 1원 미만과 병원+공용 조합을 막는다. 저장 뒤 월 합계가 한도의 90%·100%를 넘으면 한도 알림 이벤트를 발행한다
- 연결 남음: FN-MYP-031-02·03·05·06 (프론트 연결 SCR-MYP-031 직접 입력·인식 결과 확인 저장·실패 후 직접 입력의 영수증 연결·중복 지출 확인 모달, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-031-05 (영수증 저장 `ExpenseFake` → `ExpenseService.create`·`findDuplicates`, LJS) → `ReceiptSaveService.java:120`(create)·`:76`(`searchDuplicates`)에서 끝
- 연결 남음: FN-MYP-030-08 (알림 목록 COM-004에 가계부 한도 알림 표시, LJS) → 백엔드 끝(`ExpenseService.java:313·316` 발행, `NotificationService.search`), 화면 미확인

## SCR-MYP-004 공동양육자 목록 — 끝

- 기능: FN-MYP-004-01 가족 목록, FN-MYP-004-03 주양육자 표시, FN-MYP-004-04 내 연동 상태, FN-MYP-006-02 공동양육 연동 자진 해제(화면은 SCR-MYP-006), FN-MYP-006-04 목록 즉시 반영(화면은 SCR-MYP-006)
- 함께 만드는 것: 연동 양육자·연동 반려동물 조회 service 메서드(진료 사전 알림·영수증 LJS가 부름)
- 기다리는 것: 없음
- 완료 조건: 주양육자는 공동양육자를 해제할 수 있고, 공동양육자는 자기 연동만 해제할 수 있다. 해제 이력이 BRUP_LINK_STTS_HSTRY에 남는다
- 연결 남음: FN-MYP-004-01·03·04 (프론트 연결 SCR-MYP-002 슬라이드 카드 가족 영역·[✕], 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-006-02·04 (프론트 연결 SCR-MYP-006 확인 팝업·토스트, 프론트 저장소에서 /start)

## SCR-MYP-005 공동양육자 추가 — 끝

- 기능: FN-MYP-005-01 이메일 초대, FN-MYP-005-02 초대 대상 반려동물 선택, FN-MYP-005-03 보낸 초대, FN-MYP-005-04 초대 만료, FN-MYP-005-05 초대 수락 화면, FN-MYP-005-07 거절, FN-MYP-005-08 거절 확인 모달, FN-MYP-005-09 초대 이메일 일치 확인, FN-MYP-005-10 초대 만료·종료 안내
- 기다리는 것: 없음(알림·메일 이벤트는 LJS가 끝냄)
- 완료 조건: 7일 지난 초대는 수락할 수 없다. 로그인 이메일이 초대 이메일과 다르면 수락이 막힌다. 반려동물당 공동양육자 5명을 넘지 않는다
- 연결 남음: FN-MYP-005-01·02 (프론트 연결 SCR-MYP-005 5-1 초대 모달, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-005-01 (프론트 연결 SCR-MYP-005 5-1 초대 모달 409 처리를 linkedPets·pendingPets 목록으로, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-005-03·04 (프론트 연결 SCR-MYP-002 하단 보낸 초대, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-005-05·07·08·09·10 (프론트 연결 SCR-MYP-005 5-3 수락·5-4 거절 확인 모달·5-5 이메일 불일치·5-6 만료·종료 안내, 프론트 저장소에서 /start)

## SCR-MYP-030 가계부 목록 — 끝

- 기능: FN-MYP-030-01 총 지출·월 한도·진행률, FN-MYP-030-02 월간 달력, FN-MYP-030-03 날짜별 상세·수정·삭제, FN-MYP-030-04 카테고리별 지출, FN-MYP-030-05 반려동물별 지출, FN-MYP-030-06 이번달 주요 지출, FN-MYP-030-09 기간 조회·월 전환, FN-MYP-030-10 필터, FN-MYP-030-11 정렬, FN-MYP-030-12 내역 리스트, FN-MYP-030-13 한도 설정 모달, FN-MYP-030-14 한도 미설정 상태, FN-MYP-030-15 삭제 확인 모달, FN-MYP-030-16 월 피커, FN-MYP-030-17 편집 모드·삭제 범위, FN-MYP-030-18 선택·전체 선택, FN-MYP-030-19 선택 요약·삭제, FN-MYP-030-20 페이지네이션, FN-MYP-030-21 다중 삭제 확인 모달
- 기다리는 것: 없음
- 완료 조건: 월 통계가 공용 지출을 반려동물별 합계와 따로 돌려준다. 한도를 처음 저장하면 한도 알림 수신이 켜진다
- 연결 남음: FN-MYP-030-01~06·09~21 (프론트 연결 SCR-MYP-030 가계부 기본 화면·전체 내역·편집 모드·한도 설정·삭제 모달·월 피커, 프론트 저장소에서 /start)

## SCR-MYP-002 반려동물 삭제 — 끝

- 기능: FN-MYP-002-07 삭제 확인 팝업, FN-MYP-002-08 공동양육 연동 자동 해제
- 기다리는 것: 반려동물 삭제 연쇄 범위(docs/api/issues.md 마이페이지 3번, 팀)
- 완료 조건: 주양육자만 삭제할 수 있다. 삭제하면 연동이 모두 해제되고 공동양육자에게 알림이 가며, 대표였으면 다음 주양육 동물로 승계된다. 지출은 남는다
- 연결 남음: FN-MYP-002-07 (프론트 연결 SCR-MYP-002 ⋮ 메뉴 [반려동물 삭제]·2-3 삭제 확인 팝업·2-4 등록 카드 복귀, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-002-08 (알림함 COM-004에 공동양육 연동 해제 알림 표시, LJS) → 백엔드 끝(`PetService.java:273`, 반려동물 삭제 때의 알림), 화면 미확인

## SCR-MYP-040 회원정보 수정 — 끝

- 기능: FN-MYP-040-02 프로필 사진, FN-MYP-040-03 이메일(로그인 아이디), FN-MYP-040-05 휴대폰 번호 변경, FN-MYP-040-08 프로필 사진(SNS), FN-MYP-040-09 계정 표시(SNS), FN-MYP-040-11 휴대폰 번호 변경(SNS), FN-MYP-040-13 계정 정보
- API가 남의 것: FN-MYP-040-05·11 휴대폰 번호 중복 확인 `GET /members/phone-availability` (YAY)
- 기다리는 것: 닉네임·휴대전화·비밀번호 검증 (YAY), `GET /members/me` 경로 겹침(docs/api/issues.md 마이페이지 14번)
- 완료 조건: `PATCH /api/members/me`가 다른 회원이 쓰는 휴대폰 번호를 서버에서 다시 막는다. 조회가 가입 방식·가입일·최근 로그인을 돌려준다
- 연결 남음: FN-MYP-040-02·03·08·09·13 (프론트 연결 SCR-MYP-040 6-2·6-3 프로필 요약·계정 정보, 프론트 저장소에서 /start)
- 연결 남음: FN-MYP-040-02·05·08·11 (프론트 연결 SCR-MYP-040 6-2·6-3 사진 변경·기본 이미지로·휴대폰 번호 수정·저장, 프론트 저장소에서 /start)

## SCR-MYP-002 반려동물 프로필 목록 · 목록 응답에 중성화 여부·정상 체중 범위 추가 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-002-02·003-06과 걸친다
- 기다리는 것: 프론트 SCR-MYP-002 슬라이드 (프론트 저장소, 지금은 슬라이드마다 `GET /pets/{petSn}` 상세를 불러 채움)
- 완료 조건: `GET /api/pets`의 `PetSummary`에 `nutrYn`·`minBdwg`·`maxBdwg`가 들어 있다. 프론트가 상세 조회 없이 슬라이드의 "(중성화 완료)"와 "정상 a~bkg"를 채울 수 있다
- 연결 남음: FN-MYP-002-02 (프론트 연결 SCR-MYP-002 슬라이드 "(중성화 완료)"·"정상 a~bkg"를 목록 값으로, 프론트 저장소에서 /start)

## SCR-MYP-002 반려동물 삭제 · 공동양육 자동 해제 테스트 보강 — 끝

- 기능: FN-MYP-002-08 공동양육 연동 자동 해제
- 완료 조건:
  - `명령:` ./gradlew build -q 통과
  - PetServiceTest: 삭제 중 연동이 동시에 해제돼 updateBrupRelUnlink가 0이면 그 공동양육자의 이력(PETC0027)·알림을 남기지 않는다
  - PetMapperXmlTest(새로 만듦, 다른 도메인의 \*MapperXmlTest 방식): selectLinkedCoParentRels가 그 반려동물의 USE_YN='Y' 공동양육자만 고르고(주양육자·해제된 행 제외), updateBrupRelUnlink가 USE_YN='Y' 행만 해제하고 RPRS_PET_YN도 'N'으로 바꾼다
- 비고: 구현은 끝남. 이력 유형은 FORCED_UNLINKED가 아니라 PET_DELETED(PETC0027)

## SCR-MYP-005 공동양육자 추가 · 수락 카드 품종 필드 추가 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-005-05와 걸친다
- 기다리는 것: 프론트 SCR-MYP-005 수락 카드 (프론트 저장소, 끝남. 응답에 `vrtyNm`·`vrtyDrctInptNm`이 있으면 직접입력 우선으로 품종 줄을 보이게 만들어 뒀다)
- 완료 조건: `GET /api/caregiver-invitations/{jntBrupInvtnSn}`의 `pets[]`(`CaregiverInvitationPetSummary`)에 `vrtyNm`·`vrtyDrctInptNm`이 들어 있다. 보낸 초대 목록(`CaregiverInvitationSummary.pets`)도 같은 타입이므로 함께 나온다. 이메일 불일치·만료 응답에는 `pets`를 계속 내리지 않는다

## SCR-MYP-030 가계부 목록 · 일괄 삭제 제외 목록 추가·건수 불일치 409 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-030-18·19·21과 걸친다
- 기다리는 것: 프론트 SCR-MYP-030 전체 내역 (프론트 저장소, 끝남. 사람 결정 B안: 조회 결과 전체 선택 뒤 개별 해제한 건만 제외하고 지운다. 프론트는 이 API가 나온 뒤에 해제한 번호를 보내도록 바꾼다)
- 완료 조건: `POST /api/expenses/bulk-delete`가 제외 번호 목록 `excludeExpndSn`을 받고, 건수 검사 기대값 `expectedCnt`를 "조건에 맞는 건수 − 제외 수"로 비교하며, 불일치면 지금의 400(`INVALID_INPUT_VALUE`)이 아닌 409로 응답한다. 제외 번호는 조건 밖이거나 남의 지출이어도 무시하지 않고 거절 규칙을 정한다
- 결과: 잘못된 제외 번호(조건 밖·남의 지출·없는 번호·삭제된 번호)는 구별하지 않고 모두 409 `EXPENSE_BULK_DELETE_CHANGED`. 검사 뒤 삭제 건수가 기대값과 다르면 409로 롤백한다. 제외 번호는 최대 1000개
- 연결 남음: FN-MYP-030-18·19·21 (프론트 연결 SCR-MYP-030 전체 선택 뒤 해제한 번호를 `excludeExpndSn`으로 보내기·409 처리, 프론트 저장소에서 /start)

## SCR-MYP-031 가계부 등록/수정 · 등록·수정 미래 날짜 검증 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-031-02와 걸친다
- 기다리는 것: 프론트 SCR-MYP-030 (프론트 저장소, 끝남. 사람 결정 2026-11-02: 오늘(KST)까지만 허용. 프론트 폼과 달력은 이미 막았다)
- 완료 조건: `POST /api/expenses`(`ExpenseService.validate`)와 `PATCH /api/expenses/{expndSn}`(`update`)에서 `EXPND_YMD`가 오늘(KST)보다 뒤면 400(`INVALID_INPUT_VALUE`)이다
- 연결 남음: 영수증 저장 `ExpenseFake` → `ExpenseService.create` 연결 때 같은 미래 날짜 검사를 받는다 (LJS) → `ExpenseService.java:111`(`create`의 `checkNotFuture`)에서 끝

## SCR-MYP-030 가계부 목록 · 시드 병원 공용 지출 행 수정 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-031-02 "병원은 공용 불가" 규칙과 걸친다
- 기다리는 것: 프론트 SCR-MYP-030 (프론트 저장소, 끝남. 사람 확인 중 발견)
- 완료 조건: `db/seed/20261028_seed_data.sql`의 `EXPND`에서 병원(`MYPC0012`) + 공용(`PET_SN` NULL) 176행(처음 발견한 `EXPND_SN=406`, 회원 1, 2026-10-05, 42,000원 포함)의 `PET_SN`을 채워 그런 행이 없게 한다. 회원마다 사용 중·삭제 안 된 반려동물을 `PET_SN` 순으로 세워 지출일(같으면 `EXPND_SN`) 순으로 돌아가며 배정한다. 공용 DB 반영 SQL은 사람이 DBeaver로 직접 실행한다. 서버 등록·수정은 이미 `EXPENSE_PET_REQUIRED`로 막고 있어 코드 수정은 없다

## SCR-MYP-031 가계부 등록/수정 · 영수증 저장 경로 미래 날짜 검증 — 끝

- 기능: 명세 기능 ID 추가 없음. FN-MYP-031-05·FN-AIM-009-10과 걸친다
- 기다리는 것: 없음(영수증 저장은 이미 `ExpenseService.create(ExpenseCreateCommand)`를 부른다)
- 완료 조건: `ExpenseService.create(ExpenseCreateCommand)`가 `expndYmd`가 오늘(KST)보다 뒤면 400(`INVALID_INPUT_VALUE`)을 던지고, 영수증 저장 전체가 롤백된다

## SCR-HLT-002 건강 일지 · SJM 몫(PetService.syncWeight) — 끝

- 기능: 명세 기능 ID 추가 없음. FN-HLT-002-09~11 체중 기록 등록·수정·삭제 뒤 프로필 체중 동기화(화면은 SCR-HLT-002, JAJ). `PetService.syncWeight(Long petSn, BigDecimal weight, Long actorMbrSn)`를 더한다
- 기다리는 것: 없음
- 프론트 연결: 없음(서버 내부 호출). JAJ가 `PetWeightFake`를 이 메서드로 바꾼다(JAJ 마일스톤 SCR-HLT-002 · 프로필 체중 동기화 연결)
- 완료 조건: `PetService.syncWeight`가 `PET_BDWG`만 `MDFR_ID`·`MDFCN_DT`와 함께 갱신하고, 삭제됐거나 없는 반려동물이면 404 `PET_NOT_FOUND`를 던진다. 권한 검사는 하지 않는다(호출하는 쪽 담당). 단위 테스트와 `./gradlew build -q`가 통과한다
- 연결 남음: FN-HLT-002-09~11 (`PetWeightFake`를 `PetService.syncWeight`로 바꾸기, JAJ) → `DailyHealthRecordService.java:192`에서 끝

## SCR-MYP-003 반려동물 등록/수정 · 종에 맞지 않는 털색 코드 거절 — 대기

- 기능: 명세 기능 ID 추가 없음, FN-MYP-003-03에 걸침 (`PetService` 등록·수정의 털색 검증)
- 기다리는 것: 이 프론트 작업(수정 모달이 종에 맞지 않는 털색을 다시 고르게 막음). 완료됨
- 완료 조건: 강아지 전용 털색(PETC0010·0011·0012·0028~0031)을 고양이에, 고양이 전용(PETC0014~0016·0032~0034)을 강아지에 보내면 거절한다. 직접 입력(PETC0017)과 공통 털색은 어느 종에서나 받는다. 시드에서 불일치인 고양이+PETC0011 55마리는 데이터 정리 방침을 정한다

## SCR-MYP-003 반려동물 등록/수정 · 종에 맞지 않는 털색 데이터 정리 — 대기

- 기능: 명세 기능 ID 추가 없음, FN-MYP-003-03에 걸침 (시드와 공용 DB의 고양이+PETC0011 55마리)
- 기다리는 것: 이 프론트 작업(털색 선택 칸 글자색, 끝남)
- 완료 조건: 시드 파일(PET 1000행 기준)에서 고양이+PETC0011 55마리를 고양이 털색으로 고쳐 종-털색 불일치가 없게 한다. 공용 DB는 팀에 알린 뒤 반영하고, 반영 SQL은 사람이 DBeaver로 직접 실행한다. 어떤 고양이 털색으로 바꿀지는 정해야 한다

## 제외

- FN-MYP-001-02·05: 화면만 있다(더보기 펼침, 0마리 안내)
- FN-MYP-002-05·06: 화면만 있다(삭제 메뉴, 0마리 안내)
- FN-MYP-004-02: 화면만 있다(초대 카드)
- FN-MYP-005-06: 화면만 있다(나중에 하기, 초대는 대기 그대로)
- FN-MYP-006-01·03·05: 화면만 있다(확인 팝업, 취소, 완료 토스트)
- FN-MYP-010-01·02·03·04·07·08·09·10·12·13·14·16·17·18·19·23·24: 픽셀 캐릭터 API는 LJS(담당 표 "MYP-010, 05 제외")
- FN-MYP-010-15·22: 화면만 있다(상점 열기, 0마리 안내)
- FN-MYP-011-02·03·05·06: 아이템 상점 API는 LJS(담당 표 MYP-011)
- FN-MYP-011-01·04: 화면만 있다(미리보기, 담은 목록)
- FN-MYP-020-07·10: 화면만 있다(0마리 안내, 적립 안내)
- FN-MYP-030-07·22: 화면만 있다(전체 내역 이동, 모바일 상세 시트)
- FN-MYP-031-01·04: 화면만 있다(등록 방법 선택, 인식 날짜로 달력 이동)
- FN-MYP-040-01: 본인 확인(비밀번호 검증)은 YAY(`POST /members/me/password-verification`)
- FN-MYP-040-04·10: 닉네임 검증·변경은 YAY(`GET /members/nickname-availability`, docs/api/issues.md 마이페이지 13번)
- FN-MYP-040-06: 비밀번호 변경은 YAY(`PATCH /members/me/password`)
- FN-MYP-040-07·12: 화면만 있다(탈퇴 화면 SCR-ACC-020으로 이동, 탈퇴 API는 YAY)

## 대조

- 마이페이지: 명세 116 = 마일스톤 70 + 제외 46
