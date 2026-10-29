# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-003 1:1 문의 · 첨부 이미지 조회 API — 끝남 (승인 2026-10-28 16:43, 시작 2026-10-28 14:45, 검증 1회차 통과, 사람 확인 2026-10-29)
기준: docs/api/support.md(FN-CSC-003-12 행·InquiryResponse), docs/api/community.md(ImageItem), INQRY_ATCH_PHOTO·ATCH_FILE(db/20261012_create_tables_v0.9.sql). 명세 기능 ID 추가 없음(걸치는 기능 FN-CSC-003-09·12·15)
방식: 영수증 원본 이미지(`ReceiptController.getImage` → `ReceiptService.getImage` → `FileStore.load`)와 같게 한다. 새 Mapper·SQL·ErrorCode 없음
- FN-CSC-003-12·15 첨부 이미지 조회: `GET /api/inquiries/{otoInqrySn}/images/{atchFileSn}` → 200 `byte[]`(Content-Type은 파일 확장자, `Cache-Control: no-cache`, `ApiResponse`로 감싸지 않음)
  - 바꿀 파일: `support/service/InquiryService.java`에 `getImage(mbrSn, otoInqrySn, atchFileSn)`(`@Transactional(readOnly = true)`, `getOwnedOrThrow`로 본인 확인 → `inquiryMapper.selectPhotos`(DEL_YN='N')에 그 `atchFileSn`이 없으면 `FILE_NOT_FOUND` → `fileStore.load`). `support/controller/InquiryController.java`에 `getImage`
  - 오류: 없는 문의 404 `INQUIRY_NOT_FOUND`, 남의 문의 403 `FORBIDDEN`, 그 문의의 첨부가 아니거나 수정으로 뺀(DEL_YN='Y') 첨부 404 `FILE_NOT_FOUND`. 캐릭터 오류 문의의 첫 첨부(캐릭터 이미지)도 같은 API로 열린다(`load`는 공개·비공개를 가리지 않는다)
  - 문서(같은 PR): `docs/api/support.md` 표에 이 행 추가(`| GET | /inquiries/{otoInqrySn}/images/{atchFileSn} | 내 1:1 문의 첨부 이미지(본인만, 바이너리) | CSC-003-12·15 | KSJ | - | byte[] |`). `docs/api/community.md` ImageItem `atchFileSn` 설명 "이미지 조회 경로는 첨부파일 API로 만든다" → "공개 사진(펫이야기 등)은 `GET /api/files/public/{atchFileSn}`, 1:1 문의 사진은 `GET /api/inquiries/{otoInqrySn}/images/{atchFileSn}`(본인만). 채팅 사진은 그 마일스톤에서 정한다"
  - 테스트: `InquiryServiceTest`에 본인 첨부 → `StoredFile` 반환, 남의 문의 403, 다른 문의의 첨부·삭제된 첨부 404. `InquiryControllerTest`에 200과 Content-Type·본문 바이트 확인
  - 완료 조건 명령: `./gradlew build -q` 통과(위 테스트 포함)
  - 완료 조건 화면(Postman, dev-token 1번·2번 회원): 1번이 사진 2장으로 문의 작성 → 응답 `imgs[0].atchFileSn`으로 1번이 GET → 이미지가 보인다 → 2번 토큰으로 같은 GET 403 → 1번이 수정에서 그 사진을 `keepAtchFileSns`에서 빼면 같은 GET 404 → 없는 번호 404
  - 프론트 연결: SCR-CSC-003 문의 상세·수정 화면의 첨부 자리 표시를 이 경로의 이미지로 바꾼다. `<img src>`로는 Authorization 헤더를 못 보내므로 토큰을 붙여 blob으로 받아 `URL.createObjectURL`로 보여 준다 (프론트 저장소에서 /start)
이번에 하지 않는 것: 업로드 한도(10MB × 10장) 변경은 다음 마일스톤 "SCR-CSC-003 1:1 문의 · 업로드 한도"
새 의존성: 없음

## 다음에 할 것
- 다음 작업(`/start`로 기획): SCR-CSC-003 1:1 문의 · 업로드 한도(`spring.servlet.multipart.max-request-size=100MB`, `application.properties.example`과 팀원 설정 같이 변경)
- 연결 남음: FN-CSC-003-12·15 첨부 이미지 표시 (프론트 연결 SCR-CSC-003 문의 상세·수정 화면: 토큰을 붙여 blob으로 받아 `createObjectURL`로 보여 주고 클릭하면 팝업으로 크게 본다, 프론트 저장소에서 /start)
- 확인 남음: 펫이야기 글 사진 중 `PostService`가 `storePublic`으로 바꾸기 전에 `store`로 올린 것(`public/` 아님)은 `GET /api/files/public/{atchFileSn}`가 404일 수 있다
- 정리: 검증·확인용으로 만든 문의 223·225(공용 DB)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
