# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-003 1:1 문의 · 첨부 응답 원본 파일명 — 끝남 (승인 2026-10-29 11:23, 시작 2026-10-29 11:20, 검증 통과, 사람 확인 2026-10-29)
기준: docs/api/support.md(InquiryResponse), docs/api/community.md(ImageItem), INQRY_ATCH_PHOTO·ATCH_FILE.ORGNL_FILE_NM(db/20261012_create_tables_v0.9.sql). 명세 기능 ID 추가 없음(걸치는 기능 FN-CSC-003-09·12·15)
방식: 응답용 조회를 따로 둔다. `selectPhotos`(VO, 수정·이미지 조회에서 씀)는 그대로 두고, 응답만 ATCH_FILE을 JOIN한 새 조회로 만든다. VO `InquiryPhoto`에는 응답 전용 필드를 넣지 않는다
- FN-CSC-003-09·12·15 첨부 원본 파일명: `POST /api/inquiries`, `PATCH /api/inquiries/{otoInqrySn}`, `GET /api/inquiries/{otoInqrySn}` 응답 `imgs[]` 항목에 `orgnlFileNm` 추가 → `{ atchFileSn, sortSeq, orgnlFileNm }`
  - 바꿀 파일: `support/dto/InquiryImageSummary.java`를 record → `@Data` 클래스(Mapper가 채우는 목록 항목, `InquirySummary`와 같은 방식)로 바꾸고 `orgnlFileNm` 추가, `from(InquiryPhoto)` 삭제. `support/mapper/InquiryMapper.java`·`mapper/support/InquiryMapper.xml`에 `selectImageSummaries(otoInqrySn)`(`INQRY_ATCH_PHOTO P JOIN ATCH_FILE F ON F.ATCH_FILE_SN = P.ATCH_FILE_SN WHERE P.OTO_INQRY_SN = ? AND P.DEL_YN = 'N' ORDER BY P.SORT_SEQ, P.INQRY_ATCH_PHOTO_SN`. ATCH_FILE에는 DEL_YN이 없다). `support/service/InquiryService.java` `toResponse`가 이 조회를 쓴다
  - 문서(같은 PR): `docs/api/support.md` InquiryResponse `imgs` 타입 `List<ImageItem>` → `List<InquiryImageSummary>`, 그 아래에 `InquiryImageSummary` 절(atchFileSn·sortSeq는 ImageItem과 같음, `orgnlFileNm | String | Y | 첨부 팝업 표시용 원본 파일명 | ATCH_FILE.ORGNL_FILE_NM`) 추가. `docs/api/community.md` ImageItem "쓰는 DTO"에서 InquiryResponse 빼기. 이유: ImageItem은 펫이야기·채팅 등이 같이 쓰는데 원본 파일명은 문의만 필요하다
  - 테스트: `InquiryServiceTest`의 응답 검사(작성·상세·수정)를 `selectImageSummaries` 목으로 바꾸고 `imgs[0].orgnlFileNm` 확인. `InquiryControllerTest`에 `$.data.imgs[0].orgnlFileNm` 확인
  - 완료 조건 명령: `./gradlew build -q` 통과(위 테스트 포함)
  - 완료 조건 화면(Postman, dev-token 1번 회원): 사진 2장(`a.png`, `b.jpg`)으로 문의 작성 → 응답 `imgs[].orgnlFileNm`이 `a.png`, `b.jpg` → 상세 GET도 같다 → 수정에서 `a.png`를 `keepAtchFileSns`에서 빼면 응답·상세 `imgs`에 `b.jpg`만 남는다
  - 프론트 연결: SCR-CSC-003 첨부 썸네일 클릭 팝업에 `orgnlFileNm` 표시 (프론트 저장소에서 /start)
이번에 하지 않는 것: 펫이야기 등 다른 ImageItem에 원본 파일명 추가, 업로드 한도 변경(다음 마일스톤)
새 의존성: 없음

## 다음에 할 것
- 다음 작업(`/start`로 기획): SCR-CSC-003 1:1 문의 · 업로드 한도(`spring.servlet.multipart.max-request-size=100MB`, `application.properties.example`과 팀원 설정 같이 변경)
- 연결 남음: FN-CSC-003-09·12·15 첨부 썸네일 클릭 팝업에 `orgnlFileNm` 표시 (프론트 연결 SCR-CSC-003, 프론트 저장소에서 /start)
- 연결 남음: FN-CSC-003-12·15 첨부 이미지 표시 (프론트 연결 SCR-CSC-003 문의 상세·수정 화면: 토큰을 붙여 blob으로 받아 `createObjectURL`로 보여 주고 클릭하면 팝업으로 크게 본다, 프론트 저장소에서 /start)
- 확인 남음: 펫이야기 글 사진 중 `PostService`가 `storePublic`으로 바꾸기 전에 `store`로 올린 것(`public/` 아님)은 `GET /api/files/public/{atchFileSn}`가 404일 수 있다
- 정리: 검증·확인용으로 만든 문의 223·225·226·227(공용 DB, 첨부 파일 4082~4085 포함)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
