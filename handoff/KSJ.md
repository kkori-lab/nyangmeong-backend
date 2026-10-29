# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-003 1:1 문의 · 업로드 한도 — 끝남 (승인 2026-10-29 12:18, 시작 2026-10-29 12:15, 검증 통과, 사람 확인 2026-10-29)
기준: milestones/KSJ.md 완료 조건(한 장 10MB × 10장, 합계 100MB를 한 요청으로), `application.properties.example`(44·45행), `InquiryService.MAX_IMAGES = 10`, `SYS_STNG.FILE_MAX_SZ = 10485760`. 명세 기능 ID 추가 없음(걸치는 기능 FN-CSC-003-09·11·15)
현재 상태: `application.properties.example`은 이미 `max-file-size=10MB`, `max-request-size=100MB`다(95e901f, 2026-10-27, develop에 병합됨). 한도 초과는 `GlobalExceptionHandler`가 `FILE_TOO_LARGE`로 응답한다. 그래서 코드·설정 파일 변경 없이 확인만 한다
- FN-CSC-003-09·11·15 업로드 한도: `POST /api/inquiries`, `PATCH /api/inquiries/{otoInqrySn}` (multipart, `images` 최대 10장)
  - 바꿀 파일: 없음. 내 `application.properties`(git 제외)는 `max-request-size=110MB`라 예시와 다르다. 예시 값 100MB로 맞춘다(커밋되지 않는다)
  - 완료 조건 명령: `./gradlew build -q` 통과. `grep -n "multipart" src/main/resources/application.properties.example`가 `max-file-size=10MB`, `max-request-size=100MB`
  - 완료 조건 화면(Postman, dev-token 1번 회원, 서버 재시작 후): 9MB 안팎 사진 10장으로 문의 작성 → 201, `imgs` 10개 → 11MB 사진 1장으로 작성 → 400 `FILE_TOO_LARGE`(응답이 끊기지 않고 JSON으로 온다) → 사진 11장 → 400(최대 10장)
  - 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `max-request-size`를 100MB로(95e901f 커밋 본문의 "영향:"). 배포 서버 설정이 있으면 같이 바꾼다
이번에 하지 않는 것: 프록시·배포 서버의 요청 크기 제한(nginx `client_max_body_size` 등. 이 저장소에 없다), 10MB 이상 파일 지원
새 의존성: 없음

## 다음에 할 것
- 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `spring.servlet.multipart.max-request-size`를 100MB로 바꾼다(배포 서버 설정이 있으면 같이)
- 확인 남음: 펫이야기 글 사진 중 `PostService`가 `storePublic`으로 바꾸기 전에 `store`로 올린 것(`public/` 아님)은 `GET /api/files/public/{atchFileSn}`가 404일 수 있다
- 정리: 검증·확인용으로 만든 문의 223·225·226·227·229(공용 DB, 첨부 파일 4082~4085·4088~4097 포함)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
