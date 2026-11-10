# 커뮤니티(community) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /care-matches/{petPrtcMtchSn} | 매칭 조건·완료 요청 회차·사진·수령 상태 조회 | CMU-005-09·11 | KSJ | - | CareMatchResponse |
| GET | /care-recruitments | 펫돌봄 모집 목록(상태·지역·기간·동물 구분·크기 필터, 10건) | CMU-004-01·02 | KSJ | - | Page<CareRecruitmentSummary> |
| GET | /care-recruitments/minimum-wage | 최저시급 조회(로그인 회원, 관리자 설정의 현재 값을 매번 읽는다. 등록·수정 화면의 시급 안내) | CMU-004-08 | KSJ | - | MinimumWageResponse |
| GET | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 상세(상세 주소·정확 좌표 제외, 내 채팅 여부·매칭 돌보미 포함) | CMU-004-04·05 | KSJ | - | CareRecruitmentResponse |
| GET | /chat-rooms | 내 채팅 목록(`?tab=&unreadOnly=&pstSn=`. `tab`은 ALL(기본)·MINE(내 글)·APPLIED(지원한 글), 마지막 대화 최신순, 나간 방 제외) | CMU-005-02, COM-009 | KSJ | - | List<ChatRoomSummary> |
| GET | /chat-rooms/unread-count | 읽지 않은 메시지 총수(채팅 메뉴·헤더 배지) | CMU-005-03, COM-009 | KSJ | - | ChatUnreadCountResponse |
| GET | /chat-rooms/{otoChatRoomSn} | 채팅방 상단 게시글 카드·매칭 진행 상태·내 역할 조회(참여자만) | CMU-005-01·11, COM-009 | KSJ | - | ChatRoomResponse |
| GET | /chat-rooms/{otoChatRoomSn}/messages | 대화 메시지 조회(커서 방식, 최신순). `?beforeChatMsgSn=&size=30` — 처음은 `beforeChatMsgSn` 없이, 위로 스크롤하면 받은 것 중 가장 오래된 `chatMsgSn`을 넘긴다. `size` 최대 50 | CMU-005-04, COM-009 | KSJ | - | ChatMessageSliceResponse |
| GET | /community-ai-commentaries | 오늘의 이야기 조회(`?cmtryCrtrYmd=`) | CMU-001-05 | KSJ | - | CommunityAiCommentaryResponse |
| GET | /lost-reports | 실종 제보 목록(상태·지역·동물 구분·최종 목격 기간·키워드, 12건), `mineYn=true`면 내 글만(만료 글 포함) | CMU-002-01~04 | KSJ | - | Page<LostReportSummary> |
| GET | /lost-reports/{lostTpofSn} | 실종 제보 상세(목격 장소 좌표, 찾았어요 후 비공개 처리) | CMU-002-05·06 | KSJ | - | LostReportResponse |
| GET | /news | 펫뉴스 목록(쿼리 `newsClsfCd`·`keyword`·`page`·`size`, 최신순 10건, 상세 하단 최신 5건은 `size=5`) | CMU-003-01~04 | KSJ | - | Page<NewsSummary> |
| GET | /news/{newsSn} | 펫뉴스 상세 | CMU-003-04 | KSJ | - | NewsResponse |
| GET | /posts | 펫이야기 목록(게시판·정렬·키워드, 인기글 배지, 10건, `mineYn=true`면 내 글만, 항목마다 `owner`). 상세 하단 최신 5건도 사용 | CMU-001-01~04·08·10 | KSJ | - | Page<PostSummary> |
| GET | /posts/health-tips | 오늘의 건강팁 최대 3건. 관리자 지정 글은 지정 자리에, 빈 자리는 자동 선정 글(규칙은 `GET /admin/community/exposure/health-tips`) | CMU-001-07 | KSJ | - | List<PostSummary> |
| GET | /posts/popular-photo | 인기 포토(7일 → 30일 → 전체 순 대체) | CMU-001-06 | KSJ | - | PostResponse |
| GET | /posts/{pstSn} | 게시글 상세(조회수 동일 사용자 1일 1회 증가) | CMU-001-10 | KSJ | - | PostResponse |
| GET | /posts/{pstSn}/comments | 댓글·대댓글 목록(삭제 댓글 대체 문구) | CMU-001-11, CMU-002-07 | KSJ | - | List<CommentSummary> |
| POST | /care-matches | 매칭 요청(채팅방 기준, 보호자만. 요청 시점의 모집글 조건을 복사해 조건 버전을 만든다. 요청 취소된 매칭은 같은 행으로 다시 요청해 조건 버전이 늘어난다) | CMU-005-06 | KSJ | CareMatchCreateRequest | CareMatchResponse |
| POST | /care-matches/{petPrtcMtchSn}/accept | 매칭 수락(제공자만, 게시글당 1명 확정, 모집글이 매칭완료로 바뀌고 같은 글의 다른 방은 모집 종료 안내) | CMU-005-07 | KSJ | - | - |
| POST | /care-matches/{petPrtcMtchSn}/cancel | 수락 전 매칭 요청 취소(보호자만, 사유 필수) | CMU-005-06 | KSJ | CareMatchCancelRequest | - |
| POST | /care-matches/{petPrtcMtchSn}/completion-requests | 돌봄 완료 요청(제공자만. 별도 입력 없음, 서버가 회차·요청 시각을 기록. 201) | CMU-005-08 | KSJ | CareCompletionRequestCreateRequest | CareCompletionRequestResponse |
| POST | /care-matches/{petPrtcMtchSn}/confirm-completion | 보호자 돌봄 완료 확정 | CMU-005-09 | KSJ | - | - |
| POST | /care-matches/{petPrtcMtchSn}/confirm-receipt | 제공자 비용 수령 확인(양측 충족 시 거래 완료) | CMU-005-10 | KSJ | CareMatchReceiptConfirmRequest | - |
| POST | /care-recruitments | 펫돌봄 모집 등록(multipart/form-data. 반려동물·돌봄 형태·일정·비용 최저시급 검증·장소·사진·동의) | CMU-004-06~11·13 | KSJ | CareRecruitmentCreateRequest | CareRecruitmentResponse |
| POST | /chat-rooms | 게시글·보호자·지원자 조합 채팅방 생성 또는 기존 방 반환(새 방 201, 기존 방 200) | CMU-004-05, CMU-005-01 | KSJ | ChatRoomCreateRequest | ChatRoomResponse |
| POST | /chat-rooms/{otoChatRoomSn}/leave | 채팅방 나가기(내 목록에서만 숨기고 기록 유지, 진행 중 매칭은 요청 취소로 바뀌고 매칭 완료 이후였으면 모집글이 모집중으로 돌아감). 이미 나갔으면 200 | CMU-005-11 | KSJ | - | - |
| POST | /chat-rooms/{otoChatRoomSn}/messages | 메시지 전송(텍스트·사진·주소 공유, multipart/form-data, 멱등 요청 ID). 처음은 201, 같은 `idmptDmndId` 재전송은 200과 같은 응답. 저장 뒤 참여자에게 실시간으로 보낸다(아래 "실시간") | CMU-004-05, CMU-005-04·05, COM-009 | KSJ | ChatMessageCreateRequest | ChatMessageResponse |
| GET | /chat-rooms/{otoChatRoomSn}/images/{atchFileSn} | 채팅 사진 보기(참여자만, 그 방 메시지에 붙은 사진만) | CMU-005-05 | KSJ | - | 이미지 바이너리 |
| POST | /chat-rooms/{otoChatRoomSn}/read | 활성 대화창에 표시된 메시지까지 읽음 처리 | CMU-005-03, COM-009 | KSJ | ChatRoomReadRequest | - |
| POST | /lost-reports | 실종 제보 등록(게시글+제보, 품종 마스터/직접입력, 목격 좌표, 개인정보 동의) | CMU-002-10·13 | KSJ | LostReportCreateRequest | LostReportResponse |
| POST | /lost-reports/{lostTpofSn}/found | 찾았어요 전환(완료 안내 댓글 자동 생성) | CMU-002-09 | KSJ | - | - |
| POST | /lost-reports/{lostTpofSn}/renew | 만료 전 게시 기간 갱신(종료 7일 전부터 종료 전까지 한 번) | CMU-002-09 | KSJ | - | - |
| PATCH | /lost-reports/{lostTpofSn} | 실종 제보 수정(작성자, 실종 상태만, 모든 항목, 사진 유지·추가, 좌표가 바뀌면 지역 재계산) | 명세 없음(화면설계서 SCR-CMU-002-03·06) | KSJ | LostReportUpdateRequest | LostReportResponse |
| DELETE | /lost-reports/{lostTpofSn} | 실종 제보 삭제(작성자, 찾았어요·만료 글도 가능, 게시글 상태를 삭제로) | 명세 없음(화면설계서 SCR-CMU-002-03) | KSJ | - | - |
| POST | /posts | 펫이야기 게시글 작성(사진 최대 10장) | CMU-001-14 | KSJ | PostCreateRequest | PostResponse |
| POST | /posts/{pstSn}/comments | 댓글·대댓글(1depth) 등록 | CMU-001-11, CMU-002-07 | KSJ | CommentCreateRequest | CommentResponse |
| POST | /posts/{pstSn}/likes | 좋아요 등록. 멱등: 이미 누른 상태여도 200과 현재 좋아요 수(`UK_PST_RCMDTN_1`로 중복 행을 막는다) | CMU-001-09, CMU-002-08, CMU-004-12 | KSJ | - | PostLikeResponse |
| POST | /reports | 게시글·댓글 신고(사유·상세, 중복 신고 제한, 누적 처리) | CMU-001-13, CMU-002-12, CMU-004-12 | KSJ | ReportCreateRequest | ReportResponse |
| PATCH | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 수정(작성자, 모집중만, multipart/form-data) | CMU-004-11 | KSJ | CareRecruitmentUpdateRequest | CareRecruitmentResponse |
| PATCH | /posts/{pstSn} | 게시글 수정(작성자) | CMU-001-15 | KSJ | PostUpdateRequest | PostResponse |
| DELETE | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 삭제(작성자, 매칭·채팅 기록 유지) | CMU-004-11 | KSJ | - | - |
| DELETE | /posts/{pstSn} | 게시글 삭제(작성자) | CMU-001-15 | KSJ | - | - |
| DELETE | /posts/{pstSn}/comments/{cmntSn} | 댓글 삭제(답글 유지) | CMU-001-11, CMU-002-07 | KSJ | - | - |
| DELETE | /posts/{pstSn}/likes | 좋아요 취소. 멱등: 누르지 않은 상태여도 200 | CMU-001-09, CMU-002-08, CMU-004-12 | KSJ | - | - |
| GET | /admin/posts | 관리자 게시글 목록. 최신순 20건씩, 정상·블라인드·삭제 전부. `q`는 제목·작성자 닉네임 부분 일치. 시작일이 종료일보다 늦거나 코드가 틀리면 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-020-01·02 | LJS | AdminPostSearchCond | PageResponse<AdminPostSummary> |
| GET | /admin/posts/{pstSn} | 관리자 게시글 상세(상태 무관). 댓글 목록은 `GET /admin/comments?pstSn=`. 없는 글 404 POST_NOT_FOUND | ADM-020-03 | LJS | - | AdminPostDetailResponse |
| POST | /admin/posts/{pstSn}/blind | 게시글 블라인드(상태 이력, 감사 로그). 정상 글이 아니면 409 ALREADY_PROCESSED, 없는 글 404 POST_NOT_FOUND | ADM-020-04 | LJS | AdminPostStatusChangeRequest | - |
| POST | /admin/posts/{pstSn}/restore | 블라인드 복구(상태 이력, 감사 로그). 신고 누적 자동 블라인드 포함. 블라인드 글이 아니면 409 ALREADY_PROCESSED, 없는 글 404 POST_NOT_FOUND | ADM-020-05 | LJS | AdminPostStatusChangeRequest | - |
| GET | /admin/lost-reports | 관리자 실종제보 목록. 정상 게시글만(블라인드·삭제 글은 `/admin/posts`), 최신순 20건씩. 만료 기준 일시가 지난 실종 글은 만료(CMUC0008)로 내리고 필터도 같은 기준. 찾았어요 행은 `expryCrtrDt` null. 강제 만료·기간 조정 API는 없다(ADM-021-03, 결정 12). 모르는 상태 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-021-01·03 | LJS | AdminLostReportSearchCond | PageResponse<AdminLostReportSummary> |
| GET | /admin/lost-reports/{lostTpofSn} | 관리자 실종제보 상세. 실종·만료는 본문·사진·만료 기준일, 찾았어요는 제목·상태·처리 시각만(결정 13). 조회수는 올리지 않는다. 없거나 정상 글이 아니면 404 LOST_REPORT_NOT_FOUND | ADM-021-02 | LJS | - | AdminLostReportResponse |
| GET | /admin/care-matches | 관리자 펫돌봄 모집글 목록. 정상 게시글만(블라인드·삭제 글은 `/admin/posts`), 최신순 20건씩. 모르는 상태 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-022-01 | LJS | AdminCareMatchSearchCond | PageResponse<AdminCareMatchSummary> |
| GET | /admin/care-matches/{petPrtcRcrtSn} | 관리자 펫돌봄 매칭 상세(분쟁 대응, 1:1 문의의 `?rcrtSn=`도 이 번호). 모집글 정보 + 매칭 목록(최신순, 매칭마다 합의 조건 버전·상태 이력·완료 요청 회차와 사진·보호자 확인 결과). 블라인드 글도 열고, 없거나 삭제 글이면 404 CARE_RECRUITMENT_NOT_FOUND. 채팅 메시지·채팅방 번호는 주지 않는다(ADM-022-03, 결정 14·26) | ADM-022-02·03 | LJS | - | AdminCareMatchResponse |
| GET | /admin/care-matches/{petPrtcRcrtSn}/images/{atchFileSn} | 관리자 펫돌봄 완료 사진 이미지. 완료 사진은 비공개 저장이라 공개 주소(`/files/public/…`)로 열리지 않으므로 이 경로로 연다(상세 응답 `cmptnDmnds[].imgs[].atchFileSn`을 쓴다). 관리자만(일반 회원 403, 비로그인 401). 이 모집글 매칭의 완료 요청 회차 사진이 아니면 파일이 있어도 404 `FILE_NOT_FOUND`, 없거나 삭제된 모집글은 404 `CARE_RECRUITMENT_NOT_FOUND`. 응답은 이미지 바이트(`Cache-Control: no-cache`)이고 `ApiResponse`로 감싸지 않는다(문의 첨부 이미지와 같은 방식) | ADM-022-02 | KSJ | - | (이미지) |
| GET | /admin/community/exposure/health-tips | 관리자 오늘의 건강팁 현황. 노출 자리 순서 최대 3건. 지정 글(정상 펫정보, 자리 1~3)은 그 자리에, 빈 자리는 자동 선정 글(지정 안 된 정상 펫정보 글 중 제목·본문에 건강 키워드가 든 글, 좋아요순·최신순)로 앞에서부터 채운다. 건강 키워드는 `KWRD_DICT` 뉴스 분류(AIMC0039)의 건강(CMUC0018) 행(SCR-ADM-050에서 편집). 일반 회원 403 ADMIN_ONLY | ADM-023-01·03 | LJS | - | List<AdminHealthTipResponse> |
| PUT | /admin/community/exposure/health-tips/{pstSn} | 건강팁 지정(감사 로그). 같은 자리의 기존 지정은 풀리고, 다른 자리에 지정된 글이면 옮긴다. 글 검색은 `GET /admin/posts?bbsCd=CMUC0001`. 자리가 1~3이 아니거나 펫정보 글이 아니면 400 INVALID_INPUT_VALUE, 없거나 정상 글이 아니면 404 POST_NOT_FOUND | ADM-023-02 | LJS | AdminHealthTipAssignRequest | - |
| DELETE | /admin/community/exposure/health-tips/{pstSn} | 건강팁 지정 해제(감사 로그). 지정 안 된 글 409 ALREADY_PROCESSED, 없는 글 404 POST_NOT_FOUND. 지정 글이 삭제·블라인드되면 지정은 자동으로 풀린다(ADM-023-03, 복구해도 돌아오지 않음) | ADM-023-02·03 | LJS | - | - |
| GET | /admin/community/exposure/phrases | 오늘의 한마디 후보 문구 목록(사용 안 함 포함, 등록순)과 오늘 노출 문구 | ADM-023-04·05 | LJS | - | AdminTodayPhraseListResponse |
| POST | /admin/community/exposure/phrases | 오늘의 한마디 후보 문구 등록(201, 감사 로그). 문구는 앞뒤 공백을 지운 뒤 1~200자, 아니면 400 INVALID_INPUT_VALUE | ADM-023-04 | LJS | AdminTodayPhraseCreateRequest | AdminTodayPhraseCreateResponse |
| PATCH | /admin/community/exposure/phrases/{todayPhrsSn} | 오늘의 한마디 후보 문구 수정(보낸 칸만, 감사 로그). 둘 다 없거나 문구가 비거나 200자 초과면 400 INVALID_INPUT_VALUE, 없는 번호 404 TODAY_PHRASE_NOT_FOUND. 매일 00:00 배치(SYSC0030)가 사용 중 후보 1건을 무작위로 골라 그날 `CMNTY_AI_CMTRY`(출처 AIMC0029)에 저장한다. 그날 행이 있거나 후보가 없으면 저장하지 않는다(사용자 화면은 운영 설정 기본 문구) | ADM-023-04·05 | LJS | AdminTodayPhraseUpdateRequest | - |
| GET | /admin/community/exposure/commentaries | 관리자 AI 코멘트 목록. 사용 안 함 포함, 최근 일자순 20건씩 | ADM-023-06 | LJS | PageQuery | PageResponse<AdminCommentarySummary> |
| PATCH | /admin/community/exposure/commentaries/{cmntyAiCmtrySn} | AI 코멘트 사용 여부 변경(감사 로그). 끄면 사용자 `GET /community-ai-commentaries`는 운영 설정 기본 문구. 없는 번호 404 AI_COMMENTARY_NOT_FOUND | ADM-023-06 | LJS | AdminCommentaryUseRequest | - |
| GET | /admin/news | 관리자 뉴스 목록. 숨긴 기사(`useYn=false`) 포함, 등록 최신순 20건씩. 쿼리 `newsSrcCd`(CMUG07)·`newsClsfCd`(CMUG06), 없거나 공백이면 전체. 모르는 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-050-01 | LJS | AdminNewsSearchCond | PageResponse<AdminNewsSummary> |
| POST | /admin/news | 관리자 뉴스 등록(201, 감사 로그). 출처 관리자 등록(CMUC0023), 발행일시는 지금, 원문 경로 없음. 썸네일은 먼저 `POST /admin/news/images`로 올린다. 제목 1~200자·요약 1~1000자(앞뒤 공백 지운 뒤)가 아니거나 카테고리 코드가 틀리면 400 INVALID_INPUT_VALUE | ADM-050-02 | LJS | AdminNewsCreateRequest | AdminNewsCreateResponse |
| PATCH | /admin/news/{newsSn} | 뉴스 수정(보낸 칸만, 감사 로그). 제목·요약·썸네일은 관리자 등록 기사만(수집 기사에 보내면 400), 카테고리·사용 여부는 모든 기사. 숨긴 기사는 사용자 `GET /news` 목록·상세에서 빠진다. 아무 칸도 없으면 400 INVALID_INPUT_VALUE, 없는 기사 404 NEWS_NOT_FOUND | ADM-050-02·03 | LJS | AdminNewsUpdateRequest | - |
| POST | /admin/news/images | 관리자 뉴스 썸네일 업로드(201, multipart `image`). 공개 파일이라 `/api/files/public/{atchFileSn}`으로 열린다. 파일이 없으면 400 INVALID_INPUT_VALUE | ADM-050-02 | LJS | - | AdminNewsImageResponse |
| GET | /admin/news/keywords | 뉴스 분류 키워드 목록(`KWRD_DICT` 뉴스 분류 AIMC0039, 정렬순). 키워드 하나가 한 행. 수집 때 제목에 든 키워드 중 정렬 순서가 앞선 카테고리로 분류하고, 건강(CMUC0018) 키워드는 건강팁 자동 선정에도 쓴다 | ADM-050-05 | LJS | - | List<AdminNewsKeywordResponse> |
| POST | /admin/news/keywords | 뉴스 분류 키워드 등록(201, 감사 로그). 정렬 순서는 그 카테고리 기존 행과 같다(없으면 뉴스 분류 최대 + 10). 키워드 1~50자(앞뒤 공백 지운 뒤)·쉼표 없음·카테고리 CMUG06 4종이 아니면 400 INVALID_INPUT_VALUE, 같은 카테고리에 같은 키워드(대소문자 무시) 409 DUPLICATE_NEWS_KEYWORD | ADM-050-05 | LJS | AdminNewsKeywordCreateRequest | AdminNewsKeywordCreateResponse |
| DELETE | /admin/news/keywords/{kwrdDictSn} | 뉴스 분류 키워드 삭제(`DEL_YN='Y'`, 감사 로그). 다음 수집부터 쓰이지 않는다. 없거나 이미 삭제 404 NEWS_KEYWORD_NOT_FOUND | ADM-050-05 | LJS | - | - |
| GET | /admin/comments | 관리자 댓글 목록. 최신순 20건씩, 삭제된 댓글도 원문 표시. `q`는 작성자 닉네임 부분 일치 | ADM-020-06 | LJS | AdminCommentSearchCond | PageResponse<AdminCommentSummary> |
| POST | /admin/comments/{cmntSn}/deletion | 관리자 댓글 삭제. 사유는 감사 로그에 남긴다(`CMNT`에 사유 컬럼 없음). DELETE 본문 금지라 POST. 이미 삭제면 409 ALREADY_PROCESSED, 없는 댓글 404 COMMENT_NOT_FOUND | ADM-020-06 | LJS | AdminCommentDeleteRequest | - |
| GET | /admin/reports | 관리자 신고 목록. 최신순 20건씩, 같은 대상 신고 수 포함. 코드가 틀리거나 기간이 뒤집히면 400 INVALID_INPUT_VALUE | ADM-030-01·02 | LJS | AdminReportSearchCond | PageResponse<AdminReportSummary> |
| GET | /admin/reports/auto-blind | 자동 블라인드 글·자동 삭제 댓글. 대상별 1행, 대상의 최신 dclrSn으로 복구를 부른다 | ADM-030-07 | LJS | - | PageResponse<AdminReportAutoBlindSummary> |
| GET | /admin/reports/{dclrSn} | 관리자 신고 상세(대상 내용, 같은 대상 신고, 처리 이력). 없으면 404 REPORT_NOT_FOUND | ADM-030-03 | LJS | - | AdminReportDetailResponse |
| POST | /admin/reports/{dclrSn}/complete | 신고 처리 완료. 같은 대상의 접수 신고를 함께 처리하고 신고자마다 인앱 알림. 유지는 대상 변경 없음, 블라인드는 글 블라인드·댓글 삭제(이미 숨겨졌으면 신고만 처리), 작성자 정지는 회원 제재 규칙. 처리된 신고 409 ALREADY_PROCESSED, 정지할 수 없는 작성자 409 MEMBER_NOT_SUSPENDABLE | ADM-030-04·06 | LJS | AdminReportCompleteRequest | - |
| POST | /admin/reports/{dclrSn}/reject | 신고 반려. 그 신고 1건만, 사유는 이력에 남기고 신고자에게 인앱 알림. 처리된 신고 409 ALREADY_PROCESSED | ADM-030-05·06 | LJS | AdminReportRejectRequest | - |
| POST | /admin/reports/{dclrSn}/restore | 자동 블라인드 복구. 글은 정상, 댓글은 삭제 해제, 접수 신고는 반려·신고자 인앱 알림. 자동 대상이 아니면 409 ALREADY_PROCESSED | ADM-030-06·07 | LJS | AdminPostStatusChangeRequest | - |

## 실시간 (채팅, 2026-10-22 제안)

REST는 보내기·과거 조회·읽음 처리만 맡고, 새 메시지와 읽음 변경은 STOMP(WebSocket)로 받는다. 보내기를 REST로 두는 이유는 `idmptDmndId` 멱등과 `ApiResponse` 오류 응답을 그대로 쓰기 위해서다.

| 항목 | 값 |
| --- | --- |
| 연결 | `/ws` (STOMP over WebSocket, SockJS 없음). 연결(핸드셰이크)할 때 access 토큰 쿠키로 인증하고 인증이 없으면 401이다. 연결 사용자 이름은 `mbrSn`이다. 브라우저는 쿠키를 보내도록 `withCredentials`로 연결한다 |
| 구독 | `/user/queue/chat`만 구독할 수 있다. 사용자 식별자는 `mbrSn`이라 같은 회원의 모든 기기 연결로 간다 |
| 받는 것 | `{ type: "MESSAGE", otoChatRoomSn, message: ChatMessageSummary }`(`mine`은 받는 사람 기준), `{ type: "READ", otoChatRoomSn, mbrSn, msgSeq }`(`mbrSn`은 읽은 사람) |
| 보내는 것 | `/app/chat/view` 에 `{ otoChatRoomSn }`. 지금 화면에 띄워 보는 방을 알린다(방을 닫거나 창이 포커스를 잃으면 `{ otoChatRoomSn: null }`). 이 목적지 외로 보내면 거부한다 |

다중 로그인(한 회원이 여러 기기로 동시에 접속)에서 지킬 것:
- 메시지와 읽음은 회원의 모든 연결로 보낸다. 읽음 위치는 DB(회원 + 방 기준 마지막 읽은 `msgSeq`)에 두고, 한 기기에서 읽으면 다른 기기 연결에도 `READ`를 보낸다. 헤더의 채팅 안 읽은 수(`GET /chat-rooms/unread-count`)도 같은 값을 쓴다
- "채팅방을 보는 중이면 거래 알림을 만들지 않는다"(COM-004-14)는 그 회원의 연결 중 하나라도 그 방을 보고 있으면 보는 중으로 본다. 기준은 「연결 + `/app/chat/view`로 알린 방」이고, 연결이 끊기면 등록이 지워진다. 화면 포커스는 프론트가 `view`를 보내는 시점으로 정한다
- (아직 안 함) 로그아웃·세션 폐기 뒤에도 소켓이 살아 있으면 안 된다. `LoginSessionManager.revoke`가 세션 폐기 이벤트를 발행하고, 채팅 쪽이 그 `sid`의 연결을 끊는다. 지금은 `LoginSessionManager`에 이벤트가 없어 공통 모듈 변경이 필요하다. 오래 열린 연결은 access 토큰 만료 때 다시 인증한다
- 서버가 한 대면 내장 브로커로 충분하다. 여러 대가 되면 외부 브로커(Redis pub/sub 등)가 필요하다

## DTO

### AdminPostSearchCond
쓰는 API: GET /admin/posts

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| q | String | N | 제목·작성자 닉네임 부분 일치 | PST.PST_TTL, MBR.MBR_NCNM, ADM-020-02 |
| bbsCd | String | N | 게시판 코드(CMUG01). 펫돌봄은 코드가 없어 제외 | PST.BBS_CD, ADM-020-02 |
| status | String | N | 게시글 상태 코드(CMUG02) | PST.PST_STTS_CD, ADM-020-02 |
| bgngYmd | LocalDate | N | 작성 시작일 | PST.REG_DT, ADM-020-02 |
| endYmd | LocalDate | N | 작성 종료일 | PST.REG_DT, ADM-020-02 |

### AdminHealthTipResponse
쓰는 API: GET /admin/community/exposure/health-tips

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | 게시글 번호 | PST.PST_SN |
| pstTtl | String | Y | 제목 | PST.PST_TTL |
| hlthInfoExpsrSeq | Integer | N | 관리자 지정 자리(1~3). 자동 선정 글은 null | PST.HLTH_INFO_EXPSR_SEQ, ADM-023-01 |

### AdminHealthTipAssignRequest
쓰는 API: PUT /admin/community/exposure/health-tips/{pstSn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| hlthInfoExpsrSeq | Integer | Y | 노출 자리 1~3 | PST.HLTH_INFO_EXPSR_SEQ, ADM-023-02 |

### AdminTodayPhraseListResponse
쓰는 API: GET /admin/community/exposure/phrases

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| todayPhrs | Object | N | 오늘 노출 문구 `{ todayPhrsSn, phrsCn }`. 오늘 선택된(사용 중) 문구가 없으면 null. 같은 문구의 후보가 없으면(수정됨) `todayPhrsSn` null | CMNTY_AI_CMTRY, ADM-023-05 |
| content | List<AdminTodayPhraseSummary> | Y | 후보 문구 전체, 등록순 | TODAY_PHRS |

### AdminTodayPhraseSummary
쓰는 API: GET /admin/community/exposure/phrases

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| todayPhrsSn | Long | Y | 후보 문구 번호 | TODAY_PHRS.TODAY_PHRS_SN |
| phrsCn | String | Y | 문구(200자 이하) | TODAY_PHRS.PHRS_CN |
| useYn | Boolean | Y | 사용 여부. 사용 중인 문구만 매일 선택 대상 | TODAY_PHRS.USE_YN |

### AdminTodayPhraseCreateRequest
쓰는 API: POST /admin/community/exposure/phrases

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| phrsCn | String | Y | 문구. 앞뒤 공백을 지운 뒤 1~200자 | TODAY_PHRS.PHRS_CN, ADM-023-04 |
| useYn | Boolean | Y | 사용 여부 | TODAY_PHRS.USE_YN |

### AdminTodayPhraseCreateResponse
쓰는 API: POST /admin/community/exposure/phrases

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| todayPhrsSn | Long | Y | 등록한 후보 문구 번호 | TODAY_PHRS.TODAY_PHRS_SN |

### AdminTodayPhraseUpdateRequest
쓰는 API: PATCH /admin/community/exposure/phrases/{todayPhrsSn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| phrsCn | String | N | 문구. 보내면 앞뒤 공백을 지운 뒤 1~200자 | TODAY_PHRS.PHRS_CN |
| useYn | Boolean | N | 사용 여부. 둘 다 없으면 400 | TODAY_PHRS.USE_YN |

### AdminCommentarySummary
쓰는 API: GET /admin/community/exposure/commentaries

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntyAiCmtrySn | Long | Y | 코멘트 번호 | CMNTY_AI_CMTRY.CMNTY_AI_CMTRY_SN |
| cmtryCrtrYmd | LocalDate | Y | 기준 일자 | CMNTY_AI_CMTRY.CMTRY_CRTR_YMD |
| cmtryCn | String | Y | 코멘트 내용 | CMNTY_AI_CMTRY.CMTRY_CN |
| useYn | Boolean | Y | 사용 여부. 끄면 그날은 운영 설정 기본 문구 | CMNTY_AI_CMTRY.USE_YN, ADM-023-06 |

### AdminCommentaryUseRequest
쓰는 API: PATCH /admin/community/exposure/commentaries/{cmntyAiCmtrySn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| useYn | Boolean | Y | 사용 여부 | CMNTY_AI_CMTRY.USE_YN |

### AdminNewsSearchCond
쓰는 API: GET /admin/news

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSrcCd | String | N | 출처 공통코드 CMUG07(CMUC0022 외부 언론사·CMUC0023 관리자 등록). 없으면 전체 | NEWS.NEWS_SRC_CD, ADM-050-01 |
| newsClsfCd | String | N | 카테고리 공통코드 CMUG06. 없으면 전체 | NEWS.NEWS_CLSF_CD, ADM-050-01 |
| page | Integer | N | 1부터, 기본 1 | PageQuery |
| size | Integer | N | 기본 20, 최대 50 | PageQuery |

### AdminNewsSummary
쓰는 API: GET /admin/news

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |
| atchFileSn | Long | N | 관리자 등록 썸네일(`/api/files/public/{atchFileSn}`) | NEWS.ATCH_FILE_SN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD |
| newsSrcCd | String | Y | 공통코드 CMUG07 | NEWS.NEWS_SRC_CD |
| newsSrcCdNm | String | Y | 출처 이름 | COM_CD.COM_CD_NM |
| newsTtl | String | Y | | NEWS.NEWS_TTL |
| newsCn | String | Y | 요약(전체) | NEWS.NEWS_CN |
| srcPressNm | String | N | 수집 기사 언론사 | NEWS.SRC_PRESS_NM |
| orgtxtPath | String | N | 수집 기사 원문 링크, 관리자 등록은 null | NEWS.ORGTXT_PATH |
| pblcnDt | LocalDateTime | Y | 발행일시 | NEWS.PBLCN_DT |
| useYn | boolean | Y | false면 사용자 화면에서 숨김 | NEWS.USE_YN |
| regDt | LocalDateTime | Y | 등록일 | NEWS.REG_DT |
| mdfcnDt | LocalDateTime | Y | | NEWS.MDFCN_DT |
| thmbUrlPath | String | N | 수집 기사 썸네일(외부 URL) | NEWS.THMB_URL_PATH |

### AdminNewsCreateRequest
쓰는 API: POST /admin/news

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsTtl | String | Y | 앞뒤 공백 지운 뒤 1~200자 | NEWS.NEWS_TTL |
| newsCn | String | Y | 요약. 앞뒤 공백 지운 뒤 1~1000자 | NEWS.NEWS_CN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD |
| atchFileSn | Long | N | `POST /admin/news/images`로 받은 번호 | NEWS.ATCH_FILE_SN |

### AdminNewsCreateResponse
쓰는 API: POST /admin/news

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |

### AdminNewsUpdateRequest
쓰는 API: PATCH /admin/news/{newsSn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsTtl | String | N | 관리자 등록 기사만. 1~200자 | NEWS.NEWS_TTL |
| newsCn | String | N | 관리자 등록 기사만. 1~1000자 | NEWS.NEWS_CN |
| atchFileSn | Long | N | 관리자 등록 기사만. 새 썸네일 | NEWS.ATCH_FILE_SN |
| newsClsfCd | String | N | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD, ADM-050-03 |
| useYn | Boolean | N | false면 숨김. 하나도 없으면 400 | NEWS.USE_YN, ADM-050-03 |

### AdminNewsImageResponse
쓰는 API: POST /admin/news/images

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| atchFileSn | Long | Y | 등록·수정 요청의 `atchFileSn`에 넣는다 | ATCH_FILE.ATCH_FILE_SN |

### AdminNewsKeywordResponse
쓰는 API: GET /admin/news/keywords

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| kwrdDictSn | Long | Y | 삭제 경로 번호 | KWRD_DICT.KWRD_DICT_SN |
| mpngCd | String | Y | 카테고리 공통코드 CMUG06 | KWRD_DICT.MPNG_CD |
| kwrdCn | String | Y | 키워드 | KWRD_DICT.KWRD_CN |
| sortSeq | Integer | Y | 분류 우선순위(작을수록 먼저) | KWRD_DICT.SORT_SEQ |

### AdminNewsKeywordCreateRequest
쓰는 API: POST /admin/news/keywords

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| mpngCd | String | Y | 카테고리 공통코드 CMUG06(건강·생활·정책·여행) | KWRD_DICT.MPNG_CD |
| kwrdCn | String | Y | 앞뒤 공백 지운 뒤 1~50자, 쉼표 불가 | KWRD_DICT.KWRD_CN |

### AdminNewsKeywordCreateResponse
쓰는 API: POST /admin/news/keywords

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| kwrdDictSn | Long | Y | | KWRD_DICT.KWRD_DICT_SN |

### AdminLostReportSearchCond
쓰는 API: GET /admin/lost-reports

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSttsCd | String | N | 실종제보 상태 코드(CMUC0006 실종·CMUC0007 찾았어요·CMUC0008 만료). 없으면 전체. 만료는 기간 지난 실종 포함, 실종은 기간 안의 글만 | LOST_TPOF.LOST_TPOF_STTS_CD, ADM-021-01 |

### AdminLostReportSummary
쓰는 API: GET /admin/lost-reports

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | 실종제보 번호 | LOST_TPOF.LOST_TPOF_SN |
| pstSn | Long | Y | 게시글 번호(게시글·댓글 화면 이동) | LOST_TPOF.PST_SN |
| pstTtl | String | Y | 제목 | PST.PST_TTL |
| lostTpofSttsCd | String | Y | 상태 코드(계산한 만료 포함) | LOST_TPOF.LOST_TPOF_STTS_CD |
| mbrSn | Long | Y | 작성자 회원 번호 | PST.MBR_SN |
| mbrNcnm | String | Y | 작성자 닉네임 | MBR.MBR_NCNM |
| expryCrtrDt | LocalDateTime | N | 만료 기준 일시. 찾았어요는 null | LOST_TPOF.EXPRY_CRTR_DT |
| regDt | LocalDateTime | Y | 작성 일시 | PST.REG_DT |

### AdminLostReportResponse
쓰는 API: GET /admin/lost-reports/{lostTpofSn}

찾았어요 글은 `lostTpofSn`·`pstSn`·`pstTtl`·`lostTpofSttsCd`·`dscvSwtcDt`만 있고 나머지는 null. 연락처·반려동물 정보는 없다.

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | 실종제보 번호 | LOST_TPOF.LOST_TPOF_SN |
| pstSn | Long | Y | 게시글 번호 | LOST_TPOF.PST_SN |
| pstTtl | String | Y | 제목 | PST.PST_TTL |
| lostTpofSttsCd | String | Y | 상태 코드(계산한 만료 포함) | LOST_TPOF.LOST_TPOF_STTS_CD |
| dscvSwtcDt | LocalDateTime | N | 찾았어요 처리 시각 | LOST_TPOF.DSCV_SWTC_DT |
| mbrSn | Long | N | 작성자 회원 번호 | PST.MBR_SN |
| mbrNcnm | String | N | 작성자 닉네임 | MBR.MBR_NCNM |
| regDt | LocalDateTime | N | 작성 일시 | PST.REG_DT |
| pstCn | String | N | 본문 | PST.PST_CN |
| expryCrtrDt | LocalDateTime | N | 만료 기준 일시 | LOST_TPOF.EXPRY_CRTR_DT |
| imgs | List<PostImageSummary> | N | 사진 `[{ atchFileSn, sortSeq }]` | PST_IMG |

### AdminCareMatchSearchCond
쓰는 API: GET /admin/care-matches

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| rcrtSttsCd | String | N | 모집 상태 코드(PCAC0001 모집중·PCAC0002 매칭완료·PCAC0003 거래완료). 없으면 전체 | PET_PRTC_RCRT.RCRT_STTS_CD, ADM-022-01 |

### AdminCareMatchSummary
쓰는 API: GET /admin/care-matches

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcRcrtSn | Long | Y | 펫돌봄 모집 번호(상세 조회 키) | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| pstSn | Long | Y | 게시글 번호 | PET_PRTC_RCRT.PST_SN |
| pstTtl | String | Y | 제목 | PST.PST_TTL |
| rcrtSttsCd | String | Y | 모집 상태 코드(PCAG01) | PET_PRTC_RCRT.RCRT_STTS_CD |
| mbrSn | Long | Y | 작성자(보호자) 회원 번호 | PST.MBR_SN |
| mbrNcnm | String | N | 작성자 닉네임 | MBR.MBR_NCNM |
| cstAmt | BigDecimal | Y | 비용 금액 | PET_PRTC_RCRT.CST_AMT |
| cstUnitCd | String | Y | 비용 단위 코드(PCAC0004 시급·PCAC0005 건당) | PET_PRTC_RCRT.CST_UNIT_CD |
| regDt | LocalDateTime | Y | 작성 일시 | PST.REG_DT |

### AdminCareMatchResponse
쓰는 API: GET /admin/care-matches/{petPrtcRcrtSn}

`AdminCareMatchSummary`의 필드 전부 + `mtchs`. 채팅방 번호·채팅 메시지는 없다.

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| mtchs | List<Match> | Y | 매칭 목록(최신순). 없으면 빈 배열. 화면은 매칭이 여럿이면 탭으로 나눈다 | PET_PRTC_MTCH |

`Match`

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcMtchSn | Long | Y | 매칭 번호 | PET_PRTC_MTCH.PET_PRTC_MTCH_SN |
| ctptMbrSn | Long | Y | 돌봄 제공자(채팅 상대방) 회원 번호 | OTO_CHAT_ROOM.CTPT_MBR_SN |
| ctptNcnm | String | N | 돌봄 제공자 닉네임 | MBR.MBR_NCNM |
| mtchDlngSttsCd | String | Y | 매칭 거래 상태 코드(PCAG05: PCAC0012 수락 대기·0013 매칭 완료·0014 확인 대기·0015 수령 대기·0016 거래 완료·0017 요청 취소·0018 무응답 종료) | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD |
| rcvYn | Boolean | N | 비용 수령 여부(아직 없으면 null) | PET_PRTC_MTCH.RCV_YN |
| rcvIdntyDt | LocalDateTime | N | 수령 확인 일시 | PET_PRTC_MTCH.RCV_IDNTY_DT |
| regDt | LocalDateTime | Y | 매칭 요청 일시 | PET_PRTC_MTCH.REG_DT |
| cndVers | List<ConditionVersion> | Y | 합의 조건 버전(버전 내림차순) `{ cndVerNo, cstUnitCd, acrdCstAmt, acrdPrtcCn, acrdPrtcBgngDt, acrdPrtcEndDt, regDt }` | MTCH_CND_VER |
| sttsHstry | List<StatusHistory> | Y | 상태 이력(오래된 순) `{ bfrSttsCd, aftrSttsCd, cndVerNo, chgRsn, regDt }`. `cndVerNo`는 그때 기준 조건 버전(없으면 null) | PET_PRTC_MTCH_STTS_HSTRY |
| cmptnDmnds | List<CompletionRequest> | Y | 돌봄 완료 요청 회차(회차 순) `{ dmndCycl, cmptnCn, spclMttrCn, prtcEndDt, prtcrIdntyRsltCd, prtcrIdntyDt, splmntDmndRsn, regDt, imgs[{ atchFileSn, sortSeq }] }`. 보호자 확인 결과 PCAG06(PCAC0019 완료 확인·PCAC0020 보완 요청, 아직이면 null) | PRTC_CMPTN_DMND_CYCL, PRTC_CMPTN_PHOTO |

### AdminReportSearchCond
쓰는 API: GET /admin/reports

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| status | String | N | 신고처리상태공통코드(CMUG05) | DCLR.DCLR_PRCS_STTS_CD, ADM-030-02 |
| dclrRsnCd | String | N | 신고사유공통코드(CMUG04) | DCLR.DCLR_RSN_CD, ADM-030-02 |
| targetType | String | N | POST, COMMENT | ADM-030-02 |
| autoBlindYn | Boolean | N | 자동 블라인드·자동 삭제 여부 | ADM-030-02 |
| bgngYmd | LocalDate | N | 접수 시작일 | DCLR.REG_DT, ADM-030-02 |
| endYmd | LocalDate | N | 접수 종료일 | DCLR.REG_DT, ADM-030-02 |

### AdminReportSummary
쓰는 API: GET /admin/reports; GET /admin/reports/{dclrSn} (sameTargetReports)

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSn | Long | Y | | DCLR.DCLR_SN, ADM-030-01 |
| pstSn | Long | N | 대상이 게시글일 때 | DCLR.PST_SN, ADM-030-01 |
| cmntSn | Long | N | 대상이 댓글일 때 | DCLR.CMNT_SN, ADM-030-01 |
| trgtCn | String | Y | 게시글 제목 또는 댓글 내용 | PST.PST_TTL, CMNT.CMNT_CN, ADM-030-01 |
| dclrRsnCd | String | Y | | DCLR.DCLR_RSN_CD, ADM-030-01 |
| mbrSn | Long | Y | 신고자 | DCLR.MBR_SN, ADM-030-01 |
| mbrNcnm | String | Y | 신고자 닉네임 | MBR.MBR_NCNM, ADM-030-01 |
| dclrCnt | int | Y | 같은 대상 신고 수 | DCLR, ADM-030-01 |
| dclrPrcsSttsCd | String | Y | | DCLR.DCLR_PRCS_STTS_CD, ADM-030-01 |
| dclrActnCd | String | N | 신고조치공통코드(CMUG08) | DCLR.DCLR_ACTN_CD, ADM-030-04 |
| regDt | LocalDateTime | Y | 접수일 | DCLR.REG_DT, ADM-030-01 |

### AdminReportAutoBlindSummary
쓰는 API: GET /admin/reports/auto-blind

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSn | Long | Y | 대상의 최신 신고, 복구 호출에 쓴다 | DCLR.DCLR_SN, ADM-030-07 |
| pstSn | Long | N | | DCLR.PST_SN, ADM-030-07 |
| cmntSn | Long | N | | DCLR.CMNT_SN, ADM-030-07 |
| trgtCn | String | Y | 게시글 제목 또는 댓글 내용 | PST.PST_TTL, CMNT.CMNT_CN, ADM-030-07 |
| trgtMbrSn | Long | Y | 작성자 | PST.MBR_SN, CMNT.MBR_SN, ADM-030-07 |
| trgtMbrNcnm | String | Y | 작성자 닉네임 | MBR.MBR_NCNM, ADM-030-07 |
| dclrCnt | int | Y | 같은 대상 신고 수 | DCLR, ADM-030-07 |
| rcptDclrCnt | int | Y | 그중 접수 상태 수 | DCLR, ADM-030-07 |
| lastRegDt | LocalDateTime | Y | 마지막 신고 일시 | DCLR.REG_DT, ADM-030-07 |

### AdminReportDetailResponse
쓰는 API: GET /admin/reports/{dclrSn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSn | Long | Y | | DCLR.DCLR_SN, ADM-030-03 |
| dclrRsnCd | String | Y | | DCLR.DCLR_RSN_CD, ADM-030-03 |
| dclrDtlCn | String | N | | DCLR.DCLR_DTL_CN, ADM-030-03 |
| dclrPrcsSttsCd | String | Y | | DCLR.DCLR_PRCS_STTS_CD, ADM-030-03 |
| dclrActnCd | String | N | | DCLR.DCLR_ACTN_CD, ADM-030-04 |
| mbrSn | Long | Y | 신고자 | DCLR.MBR_SN, ADM-030-03 |
| mbrNcnm | String | Y | 신고자 닉네임 | MBR.MBR_NCNM, ADM-030-03 |
| prcsDt | LocalDateTime | N | | DCLR.PRCS_DT, ADM-030-03 |
| regDt | LocalDateTime | Y | | DCLR.REG_DT, ADM-030-03 |
| pstSn | Long | N | | DCLR.PST_SN, ADM-030-03 |
| cmntSn | Long | N | | DCLR.CMNT_SN, ADM-030-03 |
| pstTtl | String | N | 대상이 게시글일 때 | PST.PST_TTL, ADM-030-03 |
| pstCn | String | N | 대상이 게시글일 때 | PST.PST_CN, ADM-030-03 |
| pstSttsCd | String | N | 대상이 게시글일 때 | PST.PST_STTS_CD, ADM-030-03 |
| cmntCn | String | N | 대상이 댓글일 때 | CMNT.CMNT_CN, ADM-030-03 |
| delYn | Boolean | N | 대상이 댓글일 때 | CMNT.DEL_YN, ADM-030-03 |
| trgtMbrSn | Long | Y | 작성자 | PST.MBR_SN, CMNT.MBR_SN, ADM-030-03 |
| trgtMbrNcnm | String | Y | 작성자 닉네임 | MBR.MBR_NCNM, ADM-030-03 |
| sameTargetReports | List<AdminReportSummary> | Y | 최신순 | DCLR, ADM-030-03 |
| statusHistories | List<AdminReportStatusHistorySummary> | Y | 최신순 | DCLR_STTS_HSTRY, ADM-030-03 |

#### AdminReportStatusHistorySummary (AdminReportDetailResponse.statusHistories)

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSttsHstrySn | Long | Y | | DCLR_STTS_HSTRY.DCLR_STTS_HSTRY_SN, ADM-030-03 |
| bfrSttsCd | String | N | | DCLR_STTS_HSTRY.BFR_STTS_CD, ADM-030-03 |
| aftrSttsCd | String | Y | | DCLR_STTS_HSTRY.AFTR_STTS_CD, ADM-030-03 |
| chgRsn | String | N | | DCLR_STTS_HSTRY.CHG_RSN, ADM-030-03 |
| prcsMbrSn | Long | N | 없으면 시스템 처리 | DCLR_STTS_HSTRY.MBR_SN, ADM-030-03 |
| prcsMbrNcnm | String | N | | MBR.MBR_NCNM, ADM-030-03 |
| regDt | LocalDateTime | Y | | DCLR_STTS_HSTRY.REG_DT, ADM-030-03 |

### AdminReportCompleteRequest
쓰는 API: POST /admin/reports/{dclrSn}/complete

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrActnCd | String | Y | 신고조치공통코드(CMUG08) | DCLR.DCLR_ACTN_CD, ADM-030-04 |
| prcsMemo | String | N | 1000자 이내, 이력의 변경사유 | DCLR_STTS_HSTRY.CHG_RSN, ADM-030-04 |
| suspnDayCnt | Integer | N | 7·30, 없으면 영구 정지. 작성자 정지일 때만 | ADM-012, ADM-030-04 |
| suspnRsn | String | N | 작성자 정지면 필수 | ADM-012, ADM-030-04 |

### AdminReportRejectRequest
쓰는 API: POST /admin/reports/{dclrSn}/reject

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| rjctRsn | String | Y | 1000자 이내, 이력의 변경사유 | DCLR_STTS_HSTRY.CHG_RSN, ADM-030-05 |

### AdminPostSummary
쓰는 API: GET /admin/posts

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN, ADM-020-01 |
| bbsCd | String | Y | | PST.BBS_CD, ADM-020-01 |
| pstTtl | String | Y | | PST.PST_TTL, ADM-020-01 |
| mbrSn | Long | Y | 회원 상세 이동 값 | PST.MBR_SN, SCR-ADM-020 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-020-01 |
| pstSttsCd | String | Y | | PST.PST_STTS_CD, ADM-020-01 |
| dclrCnt | int | Y | 신고 수 | DCLR, ADM-020-01 |
| regDt | LocalDateTime | Y | | PST.REG_DT, ADM-020-01 |

### AdminPostDetailResponse
쓰는 API: GET /admin/posts/{pstSn}

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN, ADM-020-03 |
| bbsCd | String | Y | | PST.BBS_CD, ADM-020-03 |
| pstSttsCd | String | Y | | PST.PST_STTS_CD, ADM-020-03 |
| pstTtl | String | Y | | PST.PST_TTL, ADM-020-03 |
| pstCn | String | Y | | PST.PST_CN, ADM-020-03 |
| mbrSn | Long | Y | | PST.MBR_SN, ADM-020-03 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-020-03 |
| regDt | LocalDateTime | Y | | PST.REG_DT, ADM-020-03 |
| dclrCnt | int | Y | 신고 수 | DCLR, ADM-020-03 |
| cmntCnt | int | Y | 삭제 제외 댓글 수 | CMNT, ADM-020-03 |
| images | List<PostImageSummary> | Y | | PST_IMG, ADM-020-03 |
| statusHistories | List<AdminPostStatusHistorySummary> | Y | 최신순 | PST_STTS_HSTRY, ADM-020-03 |

#### AdminPostStatusHistorySummary (AdminPostDetailResponse.statusHistories)

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSttsHstrySn | Long | Y | | PST_STTS_HSTRY.PST_STTS_HSTRY_SN, ADM-020-03 |
| bfrSttsCd | String | N | | PST_STTS_HSTRY.BFR_STTS_CD, ADM-020-03 |
| aftrSttsCd | String | Y | | PST_STTS_HSTRY.AFTR_STTS_CD, ADM-020-03 |
| chgRsn | String | N | | PST_STTS_HSTRY.CHG_RSN, ADM-020-03 |
| prcsMbrSn | Long | N | 처리자. null이면 시스템 | PST_STTS_HSTRY.MBR_SN, ADM-020-03 |
| prcsMbrNcnm | String | N | 처리자 닉네임 | MBR.MBR_NCNM, ADM-020-03 |
| regDt | LocalDateTime | Y | | PST_STTS_HSTRY.REG_DT, ADM-020-03 |

### AdminPostStatusChangeRequest
쓰는 API: POST /admin/posts/{pstSn}/blind, POST /admin/posts/{pstSn}/restore

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| chgRsn | String | Y | 사유. 1000자 이내 | PST_STTS_HSTRY.CHG_RSN, ADM-020-04·05 |

### AdminCommentSearchCond
쓰는 API: GET /admin/comments

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | N | 게시글별 조회 | CMNT.PST_SN, ADM-020-06 |
| mbrSn | Long | N | 작성자별 조회 | CMNT.MBR_SN, ADM-020-06 |
| q | String | N | 작성자 닉네임 부분 일치 | MBR.MBR_NCNM, ADM-020-06 |

### AdminCommentSummary
쓰는 API: GET /admin/comments

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntSn | Long | Y | | CMNT.CMNT_SN, ADM-020-06 |
| prntCmntSn | Long | N | | CMNT.PRNT_CMNT_SN, ADM-020-06 |
| pstSn | Long | Y | | CMNT.PST_SN, ADM-020-06 |
| pstTtl | String | Y | | PST.PST_TTL, ADM-020-06 |
| mbrSn | Long | Y | | CMNT.MBR_SN, ADM-020-06 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-020-06 |
| cmntCn | String | Y | 삭제된 댓글도 원문 | CMNT.CMNT_CN, ADM-020-06 |
| delYn | boolean | Y | | CMNT.DEL_YN, ADM-020-06 |
| sysAutoCrtYn | boolean | Y | | CMNT.SYS_AUTO_CRT_YN, ADM-020-06 |
| regDt | LocalDateTime | Y | | CMNT.REG_DT, ADM-020-06 |

### AdminCommentDeleteRequest
쓰는 API: POST /admin/comments/{cmntSn}/deletion

| 필드 | 타입 | 필수 | 설명 | 근거 |
| --- | --- | --- | --- | --- |
| delRsn | String | Y | 삭제 사유. 1000자 이내 | MNGR_TASK_LOG.CHG_AFTR_CN, ADM-020-06 |

### CareCompletionRequestCreateRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/completion-requests (명세 08 「별도 입력 항목은 없다」. 돌봄 내용 `''`, 종료 시각은 요청 시각, 인계 확인 `Y`로 저장)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| idmptDmndId | String | Y | 중복 제출 방지. 최대 100자 | PRTC_CMPTN_DMND_CYCL.IDMPT_DMND_ID, CMU-005-11 |

### CareCompletionRequestResponse
쓰는 API: POST /care-matches/{petPrtcMtchSn}/completion-requests (`CareMatchResponse.cmptnDmnds`에서도 씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| prtcCmptnDmndCyclSn | Long | Y | | PRTC_CMPTN_DMND_CYCL.PRTC_CMPTN_DMND_CYCL_SN |
| petPrtcMtchSn | Long | Y | | PRTC_CMPTN_DMND_CYCL.PET_PRTC_MTCH_SN |
| mtchCndVerSn | Long | Y | 요청 당시 조건 버전 | PRTC_CMPTN_DMND_CYCL.MTCH_CND_VER_SN |
| dmndCycl | Integer | Y | 요청 회차 | PRTC_CMPTN_DMND_CYCL.DMND_CYCL, CMU-005-09 |
| prtcEndDt | LocalDateTime | Y | 요청 시각 | PRTC_CMPTN_DMND_CYCL.PRTC_END_DT |
| prtcrIdntyRsltCd | String | N | 보호자 확인 결과. 공통코드 PCAG06. 확인 전 null | PRTC_CMPTN_DMND_CYCL.PRTCR_IDNTY_RSLT_CD, CMU-005-09 |
| prtcrIdntyDt | LocalDateTime | N | | PRTC_CMPTN_DMND_CYCL.PRTCR_IDNTY_DT, CMU-005-11 |

#### ImageItem (응답 record. 펫이야기는 접미어 규칙에 맞춰 `PostImageSummary`로 쓴다. 쓰는 DTO: CareRecruitmentResponse, ChatMessageResponse, ChatMessageSummary, LostReportResponse, PostResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| atchFileSn | Long | Y | 공개 사진(펫이야기 등)은 `GET /api/files/public/{atchFileSn}`, 1:1 문의 사진은 `GET /api/inquiries/{otoInqrySn}/images/{atchFileSn}`(본인만), 채팅 사진은 `GET /api/chat-rooms/{otoChatRoomSn}/images/{atchFileSn}`(방 참여자만) | PST_IMG·CHAT_MSG_PHOTO·INQRY_ATCH_PHOTO의 ATCH_FILE_SN |
| sortSeq | Integer | Y | | 같은 테이블들의 SORT_SEQ |

### CareMatchCancelRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/cancel
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cnclRsn | String | Y | 취소 사유(칩 선택 문구 또는 직접 입력). 최대 50자, 공백만 불가. 상태 이력 `CHG_RSN`과 채팅 시스템 메시지에 남는다 | PET_PRTC_MTCH_STTS_HSTRY.CHG_RSN, CMU-005-06 |

### CareMatchCreateRequest
쓰는 API: POST /care-matches (조건은 받지 않고 요청 시점의 모집글 조건(비용·일정·돌봄 내용)을 복사한다. 화면설계서 005-03 「현재 모집글 조건으로 다시 요청했어요」)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | 이 채팅방의 게시글 작성자(보호자)만 요청 | PET_PRTC_MTCH.OTO_CHAT_ROOM_SN, CMU-005-06 |
| idmptDmndId | String | Y | 중복 요청 방지. 최대 100자 | PET_PRTC_MTCH.IDMPT_DMND_ID, CMU-005-11 |

### CareMatchReceiptConfirmRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/confirm-receipt
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rcvYn | Boolean | Y | 실제 수령 여부. false면 돌봄 완료 상태 유지 | PET_PRTC_MTCH.RCV_YN, CMU-005-10 |

### CareMatchResponse
쓰는 API: GET /care-matches/{petPrtcMtchSn}; POST /care-matches
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcMtchSn | Long | Y | | PET_PRTC_MTCH.PET_PRTC_MTCH_SN |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_MTCH.PET_PRTC_RCRT_SN |
| otoChatRoomSn | Long | Y | | PET_PRTC_MTCH.OTO_CHAT_ROOM_SN |
| mtchDlngSttsCd | String | Y | 공통코드 PCAG05 | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD, CMU-005-11 |
| rcvYn | boolean | Y | | PET_PRTC_MTCH.RCV_YN, CMU-005-10 |
| rcvIdntyDt | LocalDateTime | N | | PET_PRTC_MTCH.RCV_IDNTY_DT, CMU-005-11 |
| cnd | CareMatchConditionItem | Y | 현재 조건 버전 | MTCH_CND_VER, CMU-005-09 |
| cmptnDmnds | List<CareCompletionRequestResponse> | Y | 완료 요청 회차 목록(회차 순). 없으면 빈 목록 | PRTC_CMPTN_DMND_CYCL, CMU-005-09 |

#### CareMatchConditionItem (응답 record, 쓰는 DTO: CareMatchResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mtchCndVerSn | Long | Y | | MTCH_CND_VER.MTCH_CND_VER_SN |
| cndVerNo | Integer | Y | | MTCH_CND_VER.CND_VER_NO |
| acrdPrtcBgngDt | LocalDateTime | N | | MTCH_CND_VER.ACRD_PRTC_BGNG_DT |
| acrdPrtcEndDt | LocalDateTime | N | | MTCH_CND_VER.ACRD_PRTC_END_DT |
| schdlCnsltnPsbltyYn | boolean | Y | | MTCH_CND_VER.SCHDL_CNSLTN_PSBLTY_YN |
| acrdPrtcCn | String | Y | | MTCH_CND_VER.ACRD_PRTC_CN |
| cstUnitCd | String | Y | 공통코드 PCAG02 | MTCH_CND_VER.CST_UNIT_CD |
| acrdCstAmt | Integer | Y | 확정 금액(수령 확인 모달 표시) | MTCH_CND_VER.ACRD_CST_AMT, CMU-005-10 |

### CareRecruitmentCreateRequest
쓰는 API: POST /care-recruitments (multipart/form-data. 반려동물은 `pets[0].petSeCd`처럼 인덱스 필드로 보낸다)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pets | List<CareRecruitmentPetRequestItem> | Y | 1개 이상. 강아지·고양이 동시 선택 허용 | RCRT_PET_KND, CMU-004-06 |
| prtcShpCds | List<String> | Y | 1개 이상 복수 선택. 공통코드 PCAG03(방문돌봄·위탁돌봄·산책·놀이·훈련·그 외) | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-06 |
| prtcBgngDt | LocalDateTime | N | 일정 협의 가능이 아니면 필수. 시간은 30분 단위 | PET_PRTC_RCRT.PRTC_BGNG_DT, CMU-004-07 |
| prtcEndDt | LocalDateTime | N | 일정 협의 가능이 아니면 필수. 시작 일시보다 뒤, 30분 단위 | PET_PRTC_RCRT.PRTC_END_DT, CMU-004-07 |
| schdlCnsltnPsbltyYn | Boolean | Y | true면 일정 미정 등록 허용, 입력한 일정은 희망 일정 | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN, CMU-004-07 |
| cstUnitCd | String | Y | 공통코드 PCAG02(시급·건당) | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-08 |
| cstAmt | Integer | Y | 원 단위 양의 정수. 시급이면 최저시급(설정값) 이상. 건당은 비교하지 않는다 | PET_PRTC_RCRT.CST_AMT, SYS_STNG.LOWST_HRWG_AMT, CMU-004-08 |
| rlsRgnNm | String | Y | 공개 동네명. 최대 100자. 돌봄 지역(`rgnSn`)은 받지 않고 좌표로 서버가 계산하며 전북 밖이면 `CARE_REGION_NOT_ALLOWED`(400) | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| prtcPlcDaddr | String | Y | 상세 주소. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_DADDR_ENC, CMU-004-10 |
| prtcPlcLat | BigDecimal | Y | 정확 좌표. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_LAT, CMU-004-10 |
| prtcPlcLot | BigDecimal | Y | 정확 좌표. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_LOT, CMU-004-10 |
| pstTtl | String | Y | 최대 50자, 공백만 입력 불가(명세 기준) | PST.PST_TTL, CMU-004-09·11 |
| pstCn | String | Y | 최대 2,000자, 공백만 입력 불가(명세 기준) | PST.PST_CN, CMU-004-09·11 |
| images | List<MultipartFile> | N | 최대 10장, 장당 10MB, JPG·PNG·WEBP | PST_IMG.ATCH_FILE_SN, CMU-004-09 |
| utztnRulAgreYn | Boolean | Y | 이용수칙·정보 공개 동의. true여야 등록(issues 11) | PET_PRTC_RCRT.UTZTN_RUL_AGRE_YN, CMU-004-11 |

#### CareRecruitmentPetRequestItem (요청 record, 쓰는 DTO: CareRecruitmentCreateRequest, CareRecruitmentUpdateRequest)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01(강아지·고양이) | RCRT_PET_KND.PET_SE_CD, CMU-004-06 |
| vrtySn | Long | N | `vrtySn`·`vrtyDrctInptNm` 중 하나 필수. 해당 구분의 품종 마스터여야 한다 | RCRT_PET_KND.VRTY_SN, CMU-004-13 |
| vrtyDrctInptNm | String | N | '기타' 선택 시 직접 입력. 최대 20자(화면설계서). 마스터에 등록하지 않는다 | RCRT_PET_KND.VRTY_DRCT_INPT_NM, CMU-004-13 |
| petSizeCd | String | N | 강아지면 필수. SMALL(10kg 미만)·MEDIUM(10~25kg 미만)·LARGE(25kg 이상). 고양이는 무시. 컬럼 추가 없이 대표 체중(5·15·30kg, 고양이 0)으로 저장한다(issues 11) | RCRT_PET_KND.PET_BDWG, CMU-004-06 |

같은 `petSeCd`를 두 번 보낼 수 없다.

### CareRecruitmentResponse
쓰는 API: GET /care-recruitments/{petPrtcRcrtSn}; PATCH /care-recruitments/{petPrtcRcrtSn}; POST /care-recruitments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| pstSn | Long | Y | 좋아요·신고·채팅방 생성에 쓴다 | PET_PRTC_RCRT.PST_SN, CMU-004-12 |
| pstTtl | String | Y | | PST.PST_TTL, CMU-004-04 |
| pstCn | String | Y | | PST.PST_CN, CMU-004-04 |
| mbrSn | Long | Y | 작성자 | PST.MBR_SN, CMU-004-04 |
| mbrNcnm | String | Y | 작성자. 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, CMU-004-04 |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진. 없으면 null. 파기된 회원이면 null | MBR.ATCH_FILE_SN, CMU-004-04 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CMU-004-04 |
| inqCnt | Integer | Y | 조회수. 같은 회원은 하루 한 번 | PST.INQ_CNT, CMU-004-04 |
| rcrtSttsCd | String | Y | 공통코드 PCAG01(모집중·매칭완료·거래완료) | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-004-04 |
| pets | List<CareRecruitmentPetItem> | Y | | RCRT_PET_KND, CMU-004-04 |
| prtcShpCds | List<String> | Y | 공통코드 PCAG03 | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-04 |
| prtcBgngDt | LocalDateTime | N | | PET_PRTC_RCRT.PRTC_BGNG_DT |
| prtcEndDt | LocalDateTime | N | | PET_PRTC_RCRT.PRTC_END_DT |
| schdlCnsltnPsbltyYn | boolean | Y | | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN |
| cstUnitCd | String | Y | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD |
| cstAmt | Integer | Y | | PET_PRTC_RCRT.CST_AMT |
| rgnSn | Long | Y | | PET_PRTC_RCRT.RGN_SN |
| rlsRgnNm | String | Y | 대략적인 장소. 상세 주소·정확 좌표는 넣지 않는다 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| imgs | List<ImageItem> | Y | 없으면 빈 목록 | PST_IMG, CMU-004-04 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-004-12 |
| aplcntCnt | Integer | N | 「지금 n명과 채팅 중」. 메시지가 1건 이상 오간 채팅방 수. 작성자에게만 주고 그 밖에는 null | OTO_CHAT_ROOM.PST_SN, CMU-004-05 |
| liked | Boolean | Y | 내가 좋아요했는지 | PST_RCMDTN.USE_YN, CMU-004-12 |
| owner | Boolean | Y | 작성자 본인인지(수정·삭제 버튼) | PST.MBR_SN, CMU-004-11 |
| chatting | Boolean | Y | 내가 이 글로 채팅 중인지. 나가지 않은 참여 방 중 메시지가 1건 이상 오간 방이 있으면 true(지원자 수 `aplcntCnt`와 같은 기준, 작성자도 같은 규칙). 목록 응답에는 없다 | OTO_CHAT_ROOM.PST_SN, CHAT_PRTPNT.PTCP_STTS_CD, CMU-004-05 |
| mtchSitterNcnm | String | N | 매칭 완료·거래 완료일 때 확정된 매칭의 돌보미(채팅을 건 지원자) 닉네임. 모집중이거나 확정 매칭이 없으면 null. 돌보미가 파기된 회원(이메일 HMAC `PURGED`)이면 「탈퇴한 회원」. 상세를 보는 로그인 회원 모두에게 보인다 | PET_PRTC_RCRT.CFMTN_PET_PRTC_MTCH_SN, MBR.MBR_NCNM, CMU-004-05 |

#### CareRecruitmentPetItem (응답 record, 쓰는 DTO: CareRecruitmentResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01 | RCRT_PET_KND.PET_SE_CD |
| vrtySn | Long | N | | RCRT_PET_KND.VRTY_SN |
| vrtyNm | String | Y | `vrtySn`이 있으면 마스터 품종명, 없으면 직접입력명 | VRTY.VRTY_NM, RCRT_PET_KND.VRTY_DRCT_INPT_NM, CMU-004-13 |
| petSizeCd | String | N | 강아지면 SMALL·MEDIUM·LARGE, 고양이는 null | RCRT_PET_KND.PET_BDWG, CMU-004-06 |

### CareRecruitmentSummary
쓰는 API: GET /care-recruitments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| pstSn | Long | Y | | PET_PRTC_RCRT.PST_SN |
| pstTtl | String | Y | 1행 말줄임 | PST.PST_TTL, CMU-004-01 |
| pstCn | String | Y | 설명 2행 말줄임 | PST.PST_CN, CMU-004-01 |
| thumbnailAtchFileSn | Long | N | 첫 번째 사진. 없으면 null | PST_IMG.ATCH_FILE_SN, CMU-004-01 |
| mbrNcnm | String | Y | 작성자. 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, CMU-004-01 |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진. 없으면 null. 파기된 회원이면 null | MBR.ATCH_FILE_SN, CMU-004-01 |
| rlsRgnNm | String | Y | 지역 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-01 |
| cstUnitCd | String | Y | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-01 |
| cstAmt | Integer | Y | 보수 | PET_PRTC_RCRT.CST_AMT, CMU-004-01 |
| aplcntCnt | Integer | Y | 「채팅 n명」. 메시지가 1건 이상 오간 이 게시글의 채팅방 수 | OTO_CHAT_ROOM.PST_SN, CMU-004-01 |
| rcrtSttsCd | String | Y | 상태 배지. 공통코드 PCAG01 | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-004-01 |
| pets | List<CareRecruitmentPetItem> | Y | 반려동물 배지(구분·크기) | RCRT_PET_KND, CMU-004-01 |
| prtcShpCds | List<String> | Y | 돌봄 형태 배지. 공통코드 PCAG03 | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-01 |
| prtcBgngDt | LocalDateTime | N | 일정. 일정 협의면 null일 수 있다 | PET_PRTC_RCRT.PRTC_BGNG_DT, CMU-004-01 |
| prtcEndDt | LocalDateTime | N | | PET_PRTC_RCRT.PRTC_END_DT, CMU-004-01 |
| schdlCnsltnPsbltyYn | Boolean | Y | 일정 협의 가능 | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN, CMU-004-01 |
| regDt | LocalDateTime | Y | 경과 시간 표시 | PST.REG_DT, CMU-004-01 |
| owner | Boolean | Y | 내 글 배지 | PST.MBR_SN, CMU-004-01 |

목록 검색 조건(`CareRecruitmentSearchCond`, 쿼리): `rcrtSttsCd`(없으면 전체, `PCAC0001`이면 모집중), `mineYn`, `rgnSn`(하위 지역 포함), `prtcBgngDe`~`prtcEndDe`(일정이 기간과 겹치는 글, 일정 없는 글은 제외), `petSeCd`, `petSizeCd`(고양이만 고르면 무시). 모두 AND, 10건 최신순.
### MinimumWageResponse
쓰는 API: GET /care-recruitments/minimum-wage
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lowstHrwgAplcnYr | String | Y | 최저시급 적용 연도 | SYS_STNG.LOWST_HRWG_APLCN_YR, CMU-004-08 |
| lowstHrwgAmt | Long | Y | 시간당 최저시급(원). 관리자 설정(`PATCH /admin/settings`)을 바꾸면 다음 호출부터 바뀐 값이 나온다 | SYS_STNG.LOWST_HRWG_AMT, CMU-004-08 |

### CareRecruitmentUpdateRequest
쓰는 API: PATCH /care-recruitments/{petPrtcRcrtSn} (multipart/form-data, 모집중인 글만. 아니면 `CARE_RECRUITMENT_NOT_EDITABLE` 400)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pets | List<CareRecruitmentPetRequestItem> | N | 보내면 1개 이상. 목록 교체 방식(issues 2) | RCRT_PET_KND, CMU-004-06·11 |
| prtcShpCds | List<String> | N | 보내면 1개 이상. 목록 교체 방식 | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-06·11 |
| prtcBgngDt | LocalDateTime | N | 30분 단위 | PET_PRTC_RCRT.PRTC_BGNG_DT, CMU-004-07 |
| prtcEndDt | LocalDateTime | N | 시작 일시보다 뒤, 30분 단위 | PET_PRTC_RCRT.PRTC_END_DT, CMU-004-07 |
| schdlCnsltnPsbltyYn | Boolean | N | `true`이고 `prtcBgngDt`·`prtcEndDt`를 둘 다 보내지 않으면 기존 일정을 비운다(일정 미정). 일정을 보내면 희망 일정으로 저장한다. `false`인데 보낸 값과 저장된 값을 합친 일정이 없으면 400. 보내지 않으면 일정을 그대로 둔다 | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN, CMU-004-07 |
| cstUnitCd | String | N | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-08 |
| cstAmt | Integer | N | 원 단위 양의 정수. 시급이면 최저시급 이상 | PET_PRTC_RCRT.CST_AMT, CMU-004-08 |
| rlsRgnNm | String | N | 최대 100자 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| prtcPlcDaddr | String | N | | PET_PRTC_RCRT.PRTC_PLC_DADDR_ENC, CMU-004-10 |
| prtcPlcLat | BigDecimal | N | `prtcPlcLot`과 함께 보내며 보내면 돌봄 지역을 다시 계산한다(전북 밖이면 400) | PET_PRTC_RCRT.PRTC_PLC_LAT, CMU-004-10 |
| prtcPlcLot | BigDecimal | N | | PET_PRTC_RCRT.PRTC_PLC_LOT, CMU-004-10 |
| pstTtl | String | N | 최대 50자, 공백만 입력 불가 | PST.PST_TTL, CMU-004-09·11 |
| pstCn | String | N | 최대 2,000자, 공백만 입력 불가 | PST.PST_CN, CMU-004-09·11 |
| keepAtchFileSns | List<Long> | N | 보내면 그 목록에 없는 기존 사진은 삭제, 보내지 않으면 기존 사진 유지 | PST_IMG.ATCH_FILE_SN, CMU-004-09 |
| images | List<MultipartFile> | N | 새로 올리는 사진. 남은 사진과 합쳐 최대 10장 | PST_IMG.ATCH_FILE_SN, CMU-004-09 |

### ChatMessageCreateRequest
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| msgTypeCd | String | Y | 공통코드 CHTG03 중 `CHTC0005`(텍스트)·`CHTC0006`(사진)·`CHTC0007`(주소). 거래 카드·시스템은 서버만 만든다. 방 참여 중인 회원만 보내며 multipart/form-data | CHAT_MSG.MSG_TYPE_CD, CMU-005-04·05 |
| msgCn | String | N | 텍스트면 필수. 공백만 입력 불가, 최대 2000자(명세에 길이 없어 서버가 정함) | CHAT_MSG.MSG_CN_ENC, CMU-005-04 |
| shrnAddr | String | N | 주소 공유면 필수. 선택한 장소(이름·주소), 최대 200자. 매칭 완료·확인 대기·수령 대기에서만 보낼 수 있고 그 외는 409 `CHAT_ADDRESS_NOT_ALLOWED` | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| dtlAddr | String | N | 주소 공유의 상세 주소. 최대 30자. `shrnAddr`와 한 암호문으로 저장한다 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| images | List<MultipartFile> | N | 사진이면 필수. JPG·PNG·WEBP, 최대 10장, 장당 10MB | CHAT_MSG_PHOTO.ATCH_FILE_SN, CMU-005-05 |
| idmptDmndId | String | Y | 재시도 중복 전송 방지. 최대 100자 | CHAT_MSG.IDMPT_DMND_ID, CMU-005-04 |

### ChatMessageResponse
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| chatMsgSn | Long | Y | | CHAT_MSG.CHAT_MSG_SN |
| msgSeq | Integer | Y | 읽음 처리 기준 | CHAT_MSG.MSG_SEQ, CMU-005-03 |
| mbrSn | Long | Y | 보낸 회원 | CHAT_MSG.MBR_SN |
| mine | Boolean | Y | 본인 메시지(오른쪽 배치) | CHAT_MSG.MBR_SN, CMU-005-04 |
| msgTypeCd | String | Y | 공통코드 CHTG03 | CHAT_MSG.MSG_TYPE_CD |
| msgCn | String | N | 복호화 | CHAT_MSG.MSG_CN_ENC |
| shrnAddr | String | N | 복호화한 장소. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| dtlAddr | String | N | 복호화한 상세 주소. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| photos | List<ImageItem> | Y | 사진 메시지가 아니면 빈 목록 | CHAT_MSG_PHOTO, CMU-005-05 |
| regDt | LocalDateTime | Y | 보낸 시각 | CHAT_MSG.REG_DT |
| created | boolean | Y | 이번 요청으로 저장했는지. 같은 `idmptDmndId` 재전송은 false(200) | CHAT_MSG.IDMPT_DMND_ID, CMU-005-04 |

### ChatMessageSliceResponse
쓰는 API: GET /chat-rooms/{otoChatRoomSn}/messages (전체 건수를 세지 않는 커서 응답. `PageResponse`를 쓰지 않는다)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| content | List<ChatMessageSummary> | Y | 최신순. 없으면 빈 목록 | CMU-005-04 |
| hasNext | boolean | Y | 더 오래된 메시지가 있는지(`size + 1`건 조회로 판정) | CMU-005-04 |

### ChatMessageSummary
쓰는 API: GET /chat-rooms/{otoChatRoomSn}/messages (ChatMessageSliceResponse.content의 요소)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| chatMsgSn | Long | Y | | CHAT_MSG.CHAT_MSG_SN |
| msgSeq | Integer | Y | 읽음 처리 기준 | CHAT_MSG.MSG_SEQ, CMU-005-03 |
| mbrSn | Long | Y | 보낸 회원 | CHAT_MSG.MBR_SN |
| mine | Boolean | Y | 본인 메시지(오른쪽 배치) | CHAT_MSG.MBR_SN, CMU-005-04 |
| msgTypeCd | String | Y | 공통코드 CHTG03 | CHAT_MSG.MSG_TYPE_CD |
| msgCn | String | N | 복호화 | CHAT_MSG.MSG_CN_ENC, CMU-005-04 |
| shrnAddr | String | N | 복호화한 장소. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| dtlAddr | String | N | 복호화한 상세 주소. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| photos | List<ImageItem> | Y | 사진 메시지가 아니면 빈 목록 | CHAT_MSG_PHOTO, CMU-005-05 |
| regDt | LocalDateTime | Y | 보낸 시각 | CHAT_MSG.REG_DT |

시스템 메시지(`msgTypeCd=CHTC0009`, 요청·수락·완료 등 거래 단계 안내)는 `mbrSn`이 null이고 `mine`은 false다. 화면은 가운데 안내 줄로 그린다. 안 읽은 수에는 세지 않는다.

### ChatRoomCreateRequest
쓰는 API: POST /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | 타인의 모집중 펫돌봄 게시글. 게시글·보호자·지원자 조합에 방이 있으면 기존 방을 돌려준다 | OTO_CHAT_ROOM.PST_SN, CMU-004-05, CMU-005-01 |

### ChatRoomReadRequest
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/read
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lastPrslMsgSeq | Integer | Y | 활성 대화창에 표시된 마지막 메시지 순서. 저장값보다 작으면 무시 | CHAT_PRTPNT.LAST_PRSL_MSG_SEQ, CMU-005-03 |

### ChatRoomResponse
쓰는 API: GET /chat-rooms/{otoChatRoomSn}; POST /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | | OTO_CHAT_ROOM.OTO_CHAT_ROOM_SN |
| chatRoomSttsCd | String | Y | 공통코드 CHTG01 | OTO_CHAT_ROOM.CHAT_ROOM_STTS_CD |
| pstSn | Long | Y | 상단 게시글 카드 | OTO_CHAT_ROOM.PST_SN, CMU-005-01 |
| pstTtl | String | Y | 상단 게시글 카드 | PST.PST_TTL, CMU-005-01 |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| rcrtSttsCd | String | Y | 공통코드 PCAG01 | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-005-07 |
| ctptMbrSn | Long | Y | 상대 회원 | OTO_CHAT_ROOM.CTPT_MBR_SN |
| ctptMbrNcnm | String | Y | 상대가 파기된 회원(이메일 HMAC `PURGED`)이면 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, CMU-005-04 |
| ctptProfileAtchFileSn | Long | N | 상대 메시지 왼쪽 프로필. 상대가 파기된 회원이면 null | MBR.ATCH_FILE_SN, CMU-005-04 |
| myRole | String | Y | 이 방에서 내 역할. `GUARDIAN`(게시글 작성자) 또는 `SITTER`(지원자). 실행 버튼 결정 | CMU-005-11 |
| petPrtcMtchSn | Long | N | 이 방의 매칭. 요청 전이면 null | PET_PRTC_MTCH.PET_PRTC_MTCH_SN, CMU-005-11 |
| mtchDlngSttsCd | String | N | 매칭 진행 상태. 공통코드 PCAG05 | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD, CMU-005-11 |
| created | boolean | Y | 이번 요청으로 방을 새로 만들었는지. `POST /chat-rooms`는 새 방 201, 기존 방 200 | OTO_CHAT_ROOM, CMU-004-05 |

### ChatRoomSummary
쓰는 API: GET /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.OTO_CHAT_ROOM_SN |
| chatRoomSttsCd | String | Y | 초안 그대로. 공통코드 CHTG01 | OTO_CHAT_ROOM.CHAT_ROOM_STTS_CD |
| pstSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.PST_SN |
| pstTtl | String | Y | 초안 그대로. 1행 말줄임 | PST.PST_TTL |
| ctptMbrSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.CTPT_MBR_SN |
| ctptMbrNcnm | String | Y | 초안 그대로. 상대가 파기된 회원(이메일 HMAC `PURGED`)이면 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM |
| ctptProfileAtchFileSn | Long | N | 초안 그대로. 상대가 파기된 회원이면 null | MBR.ATCH_FILE_SN |
| myRole | String | Y | 내 역할 `GUARDIAN`·`SITTER`(닉네임 옆 「돌보미/보호자」 표시는 상대 역할이므로 반대로 그린다) | CMU-005-02 |
| mtchDlngSttsCd | String | N | 거래 상태 배지. 매칭 전이면 null. 공통코드 PCAG05 | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD, CMU-005-02 |
| lastMsgTypeCd | String | N | 마지막 메시지 유형. 없으면 null. 공통코드 CHTG03 | CHAT_MSG.MSG_TYPE_CD |
| lastMsgCn | String | N | 초안 그대로. 1행 말줄임. 사진은 「사진을 보냈어요.」, 주소는 「주소를 공유했어요.」 | CHAT_MSG.MSG_CN_ENC (복호화) |
| lastMsgDt | LocalDateTime | N | 초안 그대로 | CMU-005-02 |
| unreadCnt | Integer | Y | 초안 그대로 | CHAT_PRTPNT.LAST_PRSL_MSG_SEQ, CHAT_MSG.MSG_SEQ |

### ChatUnreadCountResponse
쓰는 API: GET /chat-rooms/unread-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| unreadCnt | Integer | Y | 초안 그대로. 실제 수를 주고 99+ 표시는 화면이 한다 | CMU-005-03 |

### CommentCreateRequest
쓰는 API: POST /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntCn | String | Y | 최대 500자, 공백만이면 400 | CMNT.CMNT_CN, CMU-001-11, CMU-002-07 |
| prntCmntSn | Long | N | 답글이면 대상 댓글. 대댓글을 고르면 그 대댓글의 최상위 댓글에 붙는다(1depth 유지, 화면설계서 "원 댓글 스레드에 이어서"). 다른 글의 댓글이거나 삭제된 댓글이면 404 | CMNT.PRNT_CMNT_SN, CMU-001-11 |

### CommentResponse
쓰는 API: POST /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntSn | Long | Y | | CMNT.CMNT_SN |
| pstSn | Long | Y | | CMNT.PST_SN |
| prntCmntSn | Long | N | | CMNT.PRNT_CMNT_SN |
| cmntCn | String | Y | | CMNT.CMNT_CN |
| mbrSn | Long | Y | | CMNT.MBR_SN |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진 번호(`GET /files/public/{번호}`). 사진이 없으면 null | MBR.ATCH_FILE_SN, 추가 요청 2026-10-30 |
| regDt | LocalDateTime | Y | 화면의 "1시간 전" 표시용 | CMNT.REG_DT, CMU-001-11 |

### CommentSummary
쓰는 API: GET /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntSn | Long | Y | | CMNT.CMNT_SN |
| prntCmntSn | Long | N | 대댓글이면 부모 댓글 | CMNT.PRNT_CMNT_SN, CMU-001-11 |
| delYn | boolean | Y | true면 화면이 "삭제된 댓글입니다"로 표시(issues 14) | CMNT.DEL_YN, CMU-001-11 |
| cmntCn | String | N | 삭제된 댓글이면 null | CMNT.CMNT_CN |
| mbrSn | Long | N | 삭제된 댓글이면 null | CMNT.MBR_SN |
| mbrNcnm | String | N | 삭제된 댓글이면 null. 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진 번호(`GET /files/public/{번호}`). 사진이 없거나 삭제된 댓글이면 null. 파기된 회원이면 null | MBR.ATCH_FILE_SN, 추가 요청 2026-10-30 |
| sysAutoCrtYn | boolean | Y | 찾았어요 완료 안내 자동 댓글 | CMNT.SYS_AUTO_CRT_YN, CMU-002-09 |
| owner | Boolean | Y | 내 댓글인지(삭제 버튼) | CMNT.MBR_SN, CMU-001-11 |
| regDt | LocalDateTime | Y | 화면의 "1시간 전" 표시용 | CMNT.REG_DT, CMU-001-11 |

### CommunityAiCommentaryResponse
쓰는 API: GET /community-ai-commentaries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntyAiCmtrySn | Long | N | 해당 일자 코멘트가 없어 기본 문구를 주면 null | CMNTY_AI_CMTRY.CMNTY_AI_CMTRY_SN |
| cmtryCrtrYmd | LocalDate | Y | | CMNTY_AI_CMTRY.CMTRY_CRTR_YMD, CMU-001-05 |
| cmtryCn | String | Y | 없으면 기본 내용 | CMNTY_AI_CMTRY.CMTRY_CN, SYS_STNG.AI_CMTRY_BSC_CN, CMU-001-05 |

### LostReportCreateRequest
쓰는 API: POST /lost-reports (multipart/form-data)
실종지역(`rgnSn`)은 받지 않는다. 서버가 목격 장소 좌표를 역지오코딩해 계산하고, 전북 밖이면 400 `LOST_REPORT_REGION_NOT_ALLOWED`다(화면설계서 SCR-CMU-002-05). `lastWtnsDt`는 ISO 일시(`2026-10-29T17:00:00`)이고 미래는 400이다.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01 | LOST_TPOF.PET_SE_CD, CMU-002-10 |
| petNm | String | Y | 최대 20자(명세) | LOST_TPOF.PET_NM, CMU-002-10 |
| vrtySn | Long | N | 선택. `vrtyDrctInptNm`과 함께 보내면 400. 둘 다 없거나 `vrtyDrctInptNm`이 빈 값이면 품종 없음(명세는 필수, 사람 결정 2026-11-06). 해당 구분의 품종 마스터여야 한다 | LOST_TPOF.VRTY_SN, CMU-002-13 |
| vrtyDrctInptNm | String | N | '기타' 선택 시 직접 입력. 최대 20자(명세). 빈 값(`""`)은 품종 없음, 공백만 있으면 400. 마스터에 등록하지 않는다 | LOST_TPOF.VRTY_DRCT_INPT_NM, CMU-002-13 |
| colrNm | String | Y | 털 색상. 최대 20자(명세) | LOST_TPOF.COLR_NM, CMU-002-10 |
| chrcCn | String | N | 특징. 최대 40자 | LOST_TPOF.CHRC_CN, CMU-002-10 |
| lastWtnsDt | LocalDateTime | Y | 마지막 목격 날짜(화면은 날짜, 컬럼은 일시, issues 12) | LOST_TPOF.LAST_WTNS_DT, CMU-002-10 |
| wtnsPlcNm | String | N | 목격 장소명. 최대 200자 | LOST_TPOF.WTNS_PLC_NM, CMU-002-10 |
| wtnsPlcLat | BigDecimal | Y | 지도 핀 좌표 | LOST_TPOF.WTNS_PLC_LAT, CMU-002-11 |
| wtnsPlcLot | BigDecimal | Y | 지도 핀 좌표 | LOST_TPOF.WTNS_PLC_LOT, CMU-002-11 |
| cnpl | String | Y | 전화번호. 숫자와 하이픈 9~13자(명세 "숫자 입력, 최대 13자") | LOST_TPOF.CNPL_ENC, CMU-002-10 |
| pstTtl | String | Y | 최대 50자(명세, 펫이야기와 동일) | PST.PST_TTL, CMU-002-10, CMU-001-14 |
| pstCn | String | Y | 최대 2000자(명세, 펫이야기와 동일) | PST.PST_CN, CMU-002-10, CMU-001-14 |
| images | List<MultipartFile> | N | multipart/form-data, 최대 10장, 장당 10MB, 확장자 검증(펫이야기와 동일). 첫 번째 사진이 대표 | PST_IMG.ATCH_FILE_SN, CMU-002-10 |
| prvcRlsAgreYn | Boolean | Y | 개인정보 수집·이용 및 게시글 공개 동의. true여야 등록 | LOST_TPOF.PRVC_RLS_AGRE_YN, CMU-002-10 |

### LostReportUpdateRequest
쓰는 API: PATCH /lost-reports/{lostTpofSn} (multipart/form-data)
`LostReportCreateRequest`와 같은 필드·같은 검증을 모두 다시 보낸다(수정 범위 전체). 아래 필드만 더한다. 상태·게시 종료일·조회수·좋아요·댓글은 바뀌지 않는다. 좌표가 그대로면 실종지역을 유지한다. 개인정보 동의는 수정 때도 true여야 한다(화면설계서 SCR-CMU-002-06 9번).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| keepAtchFileSns | List<Long> | N | 남길 기존 사진. 보내면 목록에 없는 기존 사진은 삭제, 보내지 않으면 기존 사진 유지. 남긴 수 + `images` 수가 10장 이하 | PST_IMG.ATCH_FILE_SN, 화면설계서 SCR-CMU-002-06 7번 |

### LostReportResponse
쓰는 API: GET /lost-reports/{lostTpofSn}; POST /lost-reports; PATCH /lost-reports/{lostTpofSn}
찾았어요 상태면 작성자가 아닌 회원에게 제목·상태·처리 시각만 주고 나머지 N 필드는 null이다(CMU-002-09).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | | LOST_TPOF.LOST_TPOF_SN |
| pstSn | Long | Y | 좋아요·댓글·신고에 쓴다 | LOST_TPOF.PST_SN |
| mbrSn | Long | N | 작성자 회원 번호(댓글 「작성자」 배지). 찾았어요로 닫힌 응답은 null | PST.MBR_SN, CMU-002-07, 추가 요청 2026-10-30 |
| mbrNcnm | String | N | 작성자 닉네임(화면 "토리맘 · 등록일시"). 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, 화면설계서 SCR-CMU-002-02 |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진 번호(`GET /files/public/{번호}`). 사진이 없거나 찾았어요로 닫힌 응답이면 null. 파기된 회원이면 null | MBR.ATCH_FILE_SN, 추가 요청 2026-10-30 |
| regDt | LocalDateTime | N | 게시글 등록 일시 | PST.REG_DT, 화면설계서 SCR-CMU-002-02 |
| pstTtl | String | Y | | PST.PST_TTL, CMU-002-09 |
| lostTpofSttsCd | String | Y | 공통코드 CMUG03(실종·찾았어요) | LOST_TPOF.LOST_TPOF_STTS_CD, CMU-002-09 |
| dscvSwtcDt | LocalDateTime | N | 찾았어요 전환 시각 | LOST_TPOF.DSCV_SWTC_DT, CMU-002-09 |
| pstCn | String | N | | PST.PST_CN, CMU-002-05 |
| petSeCd | String | N | 공통코드 PETG01 | LOST_TPOF.PET_SE_CD, CMU-002-05 |
| petNm | String | N | | LOST_TPOF.PET_NM |
| vrtySn | Long | N | | LOST_TPOF.VRTY_SN |
| vrtyNm | String | N | `vrtySn`이 있으면 마스터 품종명, 없으면 직접입력명 | VRTY.VRTY_NM, LOST_TPOF.VRTY_DRCT_INPT_NM, CMU-002-13 |
| colrNm | String | N | | LOST_TPOF.COLR_NM, CMU-002-05 |
| chrcCn | String | N | | LOST_TPOF.CHRC_CN |
| lastWtnsDt | LocalDateTime | N | | LOST_TPOF.LAST_WTNS_DT, CMU-002-05 |
| rgnSn | Long | N | | LOST_TPOF.RGN_SN |
| rgnNm | String | N | 시도·시군구·읍면동 조합 | RGN.CTPV_NM, RGN.SGG_NM, RGN.EMD_NM |
| wtnsPlcNm | String | N | | LOST_TPOF.WTNS_PLC_NM, CMU-002-05 |
| wtnsPlcLat | BigDecimal | N | 지도 표시 | LOST_TPOF.WTNS_PLC_LAT, CMU-002-06 |
| wtnsPlcLot | BigDecimal | N | 지도 표시 | LOST_TPOF.WTNS_PLC_LOT, CMU-002-06 |
| cnpl | String | N | 작성자 입력값 그대로(복호화). 로그인 회원만 | LOST_TPOF.CNPL_ENC, CMU-002-05 |
| expryCrtrDt | LocalDateTime | N | 실종 상태 만료 기준 일시(갱신 버튼). 작성자에게만 준다 | LOST_TPOF.EXPRY_CRTR_DT, CMU-002-09 |
| imgs | List<ImageItem> | N | | PST_IMG, CMU-002-05 |
| inqCnt | Integer | N | | PST.INQ_CNT, CMU-002-07 |
| rcmdtnCnt | Integer | N | | PST.RCMDTN_CNT, CMU-002-08 |
| liked | Boolean | Y | | PST_RCMDTN.USE_YN, CMU-002-08 |
| owner | Boolean | Y | 작성자 본인인지(찾았어요·갱신 버튼) | PST.MBR_SN, CMU-002-09 |

### LostReportSummary
쓰는 API: GET /lost-reports
찾았어요 상태 카드는 작성자가 아니면 N 필드가 null이다(CMU-002-09).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | | LOST_TPOF.LOST_TPOF_SN |
| pstTtl | String | Y | | PST.PST_TTL, CMU-002-09 |
| lostTpofSttsCd | String | Y | 배지·톤다운 구분. 공통코드 CMUG03. 만료(CMUC0008)는 `mineYn=true` 목록에서만 나온다 | LOST_TPOF.LOST_TPOF_STTS_CD, CMU-002-02 |
| dscvSwtcDt | LocalDateTime | N | | LOST_TPOF.DSCV_SWTC_DT, CMU-002-09 |
| petSeCd | String | N | 공통코드 PETG01. 카드의 강아지·고양이 구분 | LOST_TPOF.PET_SE_CD, CMU-002-01 |
| petNm | String | N | 이름 | LOST_TPOF.PET_NM, CMU-002-01 |
| vrtyNm | String | N | 품종. 마스터 품종명, 없으면 직접입력명(카드 "보리 (몰티즈)") | VRTY.VRTY_NM, LOST_TPOF.VRTY_DRCT_INPT_NM, 화면설계서 SCR-CMU-002-01 |
| thumbAtchFileSn | Long | N | 대표 사진(첫 번째). 사진이 없으면 null | PST_IMG.ATCH_FILE_SN, 화면설계서 SCR-CMU-002-01 4번 |
| regDt | LocalDateTime | Y | 게시글 등록 일시(카드 "2시간 전") | PST.REG_DT, 화면설계서 SCR-CMU-002-01 |
| rgnNm | String | N | 지역. 시도·시군구·읍면동 조합 | RGN.CTPV_NM, RGN.SGG_NM, RGN.EMD_NM, CMU-002-01 |
| colrNm | String | N | 털색상 | LOST_TPOF.COLR_NM, CMU-002-01 |
| chrcCn | String | N | 특징 | LOST_TPOF.CHRC_CN, CMU-002-01 |
| owner | Boolean | Y | 내가 쓴 글이면 true(「내 글」 배지). `mineYn` 없는 전체 목록에서도 채운다. 상세 응답의 owner와 같은 이름 | PST.MBR_SN, 추가 요청 2026-10-30 |

### NewsResponse
쓰는 API: GET /news/{newsSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD |
| newsSrcCd | String | Y | 공통코드 CMUG07(외부 언론사·관리자 등록) | NEWS.NEWS_SRC_CD, CMU-003-04 |
| newsSrcCdNm | String | Y | 출처 표시 | COM_CD.COM_CD_NM, CMU-003-04 |
| srcPressNm | String | N | 외부 기사 언론사 | NEWS.SRC_PRESS_NM, CMU-003-04 |
| newsTtl | String | Y | | NEWS.NEWS_TTL |
| newsCn | String | Y | 외부 기사는 요약만 | NEWS.NEWS_CN, CMU-003-04·06 |
| orgtxtPath | String | N | 원문보기 링크 | NEWS.ORGTXT_PATH, CMU-003-04 |
| pblcnDt | LocalDateTime | Y | 작성일 | NEWS.PBLCN_DT, CMU-003-04 |
| thmbUrlPath | String | N | 외부 기사 썸네일 URL | NEWS.THMB_URL_PATH, CMU-003-06 |
| atchFileSn | Long | N | 관리자 등록 뉴스 이미지 | NEWS.ATCH_FILE_SN |

### NewsSummary
쓰는 API: GET /news
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD, CMU-003-02 |
| newsSrcCd | String | Y | 공통코드 CMUG07 | NEWS.NEWS_SRC_CD, CMU-003-01 |
| newsSrcCdNm | String | Y | 출처 배지 | COM_CD.COM_CD_NM, CMU-003-01 |
| srcPressNm | String | N | 외부 기사 언론사(목록 카드의 언론사명) | NEWS.SRC_PRESS_NM, CMU-003-01 |
| newsTtl | String | Y | | NEWS.NEWS_TTL, CMU-003-01 |
| newsCn | String | Y | 요약 줄, 앞 200자 | NEWS.NEWS_CN, CMU-003-01 |
| pblcnDt | LocalDateTime | Y | 최신순 기준 | NEWS.PBLCN_DT, CMU-003-01 |
| thmbUrlPath | String | N | | NEWS.THMB_URL_PATH, CMU-003-06 |
| atchFileSn | Long | N | | NEWS.ATCH_FILE_SN |

### PostCreateRequest
쓰는 API: POST /posts
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bbsCd | String | Y | 공통코드 CMUG01(정보·일상) | PST.BBS_CD, CMU-001-14 |
| pstTtl | String | Y | 최대 50자 | PST.PST_TTL, CMU-001-14 |
| pstCn | String | Y | 최대 2000자 | PST.PST_CN, CMU-001-14 |
| images | List<MultipartFile> | N | multipart/form-data. 최대 10장, 장당 10MB, jpg·jpeg·png·webp. 첫 장이 대표 사진. 공용 업로드 API가 없어 파일을 같이 받는다 | PST_IMG.ATCH_FILE_SN, CMU-001-14 |

### PostLikeResponse
쓰는 API: POST /posts/{pstSn}/likes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST_RCMDTN.PST_SN |
| rcmdtnCnt | Integer | Y | 반영 후 좋아요 수 | PST.RCMDTN_CNT, CMU-001-09 |

### PostResponse
쓰는 API: GET /posts/popular-photo; GET /posts/{pstSn}; PATCH /posts/{pstSn}; POST /posts
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN |
| bbsCd | String | Y | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-10 |
| pstTtl | String | Y | | PST.PST_TTL, CMU-001-10 |
| pstCn | String | Y | | PST.PST_CN, CMU-001-10 |
| mbrSn | Long | Y | 작성자 | PST.MBR_SN, CMU-001-10 |
| mbrNcnm | String | Y | 작성자. 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, CMU-001-10 |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진 번호(`GET /files/public/{번호}`). 사진이 없으면 null. 파기된 회원이면 null(정지 회원, 탈퇴했지만 파기 전인 회원은 그대로) | MBR.ATCH_FILE_SN, 추가 요청 2026-10-30 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CMU-001-10 |
| inqCnt | Integer | Y | | PST.INQ_CNT, CMU-001-10 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-001-09 |
| cmntCnt | Integer | Y | 삭제 댓글 제외 | CMNT.DEL_YN, CMU-001-11 |
| imgs | List<ImageItem> | Y | 없으면 빈 목록. 인기 포토는 첫 장 표시 | PST_IMG, CMU-001-06·10 |
| liked | Boolean | Y | 내가 좋아요했는지 | PST_RCMDTN.USE_YN, CMU-001-09 |
| owner | Boolean | Y | 작성자 본인인지(수정·삭제) | PST.MBR_SN, CMU-001-15 |

### PostSummary
쓰는 API: GET /posts; GET /posts/health-tips
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN |
| bbsCd | String | Y | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-02 |
| pstTtl | String | Y | 1행 말줄임 | PST.PST_TTL, CMU-001-01 |
| pstCn | String | Y | 2행 말줄임 | PST.PST_CN, CMU-001-01 |
| mbrNcnm | String | Y | 작성자. 파기된 회원(이메일 HMAC `PURGED`)은 「탈퇴한 회원」(탈퇴했지만 파기 전이면 원래 닉네임) | MBR.MBR_NCNM, CMU-001-01 |
| mbrAtchFileSn | Long | N | 작성자 프로필 사진 번호(`GET /files/public/{번호}`). 사진이 없으면 null. 파기된 회원이면 null(정지 회원, 탈퇴했지만 파기 전인 회원은 그대로) | MBR.ATCH_FILE_SN, 추가 요청 2026-10-30 |
| regDt | LocalDateTime | Y | 작성일("1시간 전" 표시용) | 공통 감사 컬럼 REG_DT, CMU-001-01 |
| thumbAtchFileSn | Long | N | 대표 사진(첫 장). 사진이 없으면 null | PST_IMG.ATCH_FILE_SN, CMU-001-01 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-001-03 |
| cmntCnt | Integer | Y | 삭제 댓글 제외 | CMNT.DEL_YN, CMU-001-11 |
| popular | Boolean | Y | 인기글 배지(최근 7일, 좋아요 1개 이상 상위 5개). 인기순 정렬이면 false | CMU-001-03·04 |
| owner | Boolean | N | 내 글이면 true. 목록(`GET /posts`)에서만 채우고 건강팁·인기 포토는 null | PST.MBR_SN, 사람 요청 2026-11-10 |

### PostUpdateRequest
쓰는 API: PATCH /posts/{pstSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bbsCd | String | N | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-15 |
| pstTtl | String | N | 최대 50자, 공백만이면 400 | PST.PST_TTL, CMU-001-14·15 |
| pstCn | String | N | 최대 2000자, 공백만이면 400 | PST.PST_CN, CMU-001-14·15 |
| keepAtchFileSns | List<Long> | N | 남길 기존 사진. 보내면 목록에 없는 기존 사진은 삭제(DEL_YN='Y'), 안 보내면 모두 유지 | PST_IMG.ATCH_FILE_SN, CMU-001-15 |
| images | List<MultipartFile> | N | multipart/form-data. 새로 추가할 사진. 남은 사진과 합쳐 최대 10장 | PST_IMG.ATCH_FILE_SN, CMU-001-15 |

### ReportCreateRequest
쓰는 API: POST /reports
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | N | `pstSn`·`cmntSn` 중 하나만 필수 | DCLR.PST_SN, CMU-001-13 |
| cmntSn | Long | N | 같은 회원의 같은 댓글 중복 신고 불가 | DCLR.CMNT_SN, CMU-001-13 |
| dclrRsnCd | String | Y | 공통코드 CMUG04(스팸·광고성/욕설·비방/음란물·유해콘텐츠/개인정보 노출/사기·허위 정보/기타) | DCLR.DCLR_RSN_CD, CMU-001-13 |
| dclrDtlCn | String | N | 최대 500자. 사유가 기타(CMUC0014)면 필수 | DCLR.DCLR_DTL_CN, CMU-001-13 |

### ReportResponse
쓰는 API: POST /reports
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSn | Long | Y | | DCLR.DCLR_SN |
| dclrPrcsSttsCd | String | Y | 공통코드 CMUG05 | DCLR.DCLR_PRCS_STTS_CD, CMU-004-12 |
