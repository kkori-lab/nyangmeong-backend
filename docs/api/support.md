# 고객센터(support) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /faqs | FAQ 목록(쿼리 `faqClsfCd`·`keyword`·`page`·`size`, 카테고리·키워드는 AND, 답변 포함, 10건) | CSC-002-01~03·05 | KSJ | - | Page<FaqSummary> |
| GET | /inquiries | 내 1:1 문의 목록(처리 상태 필터) | CSC-003-01~04·16·17 | KSJ | - | Page<InquirySummary> |
| GET | /inquiries/{otoInqrySn} | 내 1:1 문의 상세(답변 포함) | CSC-003-12·13·16 | KSJ | - | InquiryResponse |
| GET | /inquiries/{otoInqrySn}/images/{atchFileSn} | 내 1:1 문의 첨부 이미지(본인만, 이미지 바이너리, 다른 문의·삭제된 첨부는 404) | CSC-003-12·15 | KSJ | - | byte[] |
| GET | /notices | 공지사항 목록(상단 고정 우선·제목·내용 검색 `keyword`, 10건). 쿼리 `latestYn=true`면 고정을 무시하고 작성일 최신순이라 상세 하단 최신 5건은 `?latestYn=true&size=5`로 부른다 | CSC-001-01~04·06 | KSJ | - | Page<NoticeSummary> |
| GET | /notices/{ntcMttrSn} | 공지사항 상세(조회수는 쓰지 않음, 2026-10-30 사람 결정) | CSC-001-05 | KSJ | - | NoticeResponse |
| POST | /inquiries | 1:1 문의 작성(multipart, 캐릭터 오류 문의 연동 포함, 접수 상태) | CSC-003-06·08~11, MYP-010-12·24 (기능 분배 3.1절 21·24번) | KSJ | InquiryCreateRequest | InquiryResponse |
| DELETE | /inquiries/{otoInqrySn} | 1:1 문의 삭제(본인, 접수 상태만, 캐릭터 오류 문의 제외, DEL_YN) | 명세 없음(프론트 요청) | KSJ | - | - |
| PATCH | /inquiries/{otoInqrySn} | 1:1 문의 수정(multipart, 접수 상태에서만) | CSC-003-15 | KSJ | InquiryUpdateRequest | InquiryResponse |
| GET | /admin/faqs | 관리자 FAQ 목록. 비노출 포함, 정렬 순서 다음 번호 오름차순, 20건씩. `faqClsfCd`(CSCG01)와 `keyword`(질문·답변 부분 일치)는 AND. 없는 카테고리 코드는 400 INVALID_INPUT_VALUE. 카테고리 탭은 `GET /common-codes?prntComCd=CSCG01`. 일반 회원 403 ADMIN_ONLY | ADM-041-01·04 | LJS | AdminFaqSearchCond | PageResponse<AdminFaqSummary> |
| GET | /admin/faqs/{faqSn} | 관리자 FAQ 상세(수정 모달, 사용 여부 무관). 없으면 404 FAQ_NOT_FOUND | ADM-041-02 | LJS | - | AdminFaqResponse |
| POST | /admin/faqs | FAQ 등록(201, 감사 로그). 정렬 순서는 중복 허용, 다른 FAQ를 밀지 않는다. 없는 카테고리 코드는 400 INVALID_INPUT_VALUE | ADM-041-02·04 | LJS | AdminFaqCreateRequest | AdminFaqResponse |
| PATCH | /admin/faqs/{faqSn} | FAQ 수정·비노출(감사 로그, 보낸 값만 바꾼다). 삭제 API는 없고 `useYn=false`로 비노출하면 `GET /faqs`에서 빠진다. 없으면 404 FAQ_NOT_FOUND | ADM-041-02·03 | LJS | AdminFaqUpdateRequest | AdminFaqResponse |
| GET | /admin/notices | 관리자 공지 목록. 비노출 포함, 고정 먼저 그다음 등록일 최신순, 20건씩. `keyword`는 제목 부분 일치. 일반 회원 403 ADMIN_ONLY | ADM-040-01·05 | LJS | AdminNoticeSearchCond | PageResponse<AdminNoticeSummary> |
| GET | /admin/notices/{ntcMttrSn} | 관리자 공지 상세(수정 화면, 사용 여부 무관). 없으면 404 NOTICE_NOT_FOUND | ADM-040-04 | LJS | - | AdminNoticeResponse |
| GET | /admin/notices/notification-target-count | 공지 알림 대상 수(정상 회원 수). 발송 확인 모달 "회원 N명에게 알림을 보내요" | ADM-040-03 | LJS | - | AdminNoticeTargetCountResponse |
| POST | /admin/notices | 공지 등록(201, 감사 로그). `notiSndngYn=true`면 정상 회원마다 공지 인앱 알림을 한 번 보내고 발송 시각·대상 수를 남긴다(앱 알림 채널을 끈 회원은 받지 않지만 대상 수에는 든다). `useYn=false`와 함께 보내면 400 INVALID_INPUT_VALUE | ADM-040-02·03·05 | LJS | AdminNoticeCreateRequest | AdminNoticeResponse |
| PATCH | /admin/notices/{ntcMttrSn} | 공지 수정·비노출(감사 로그). 알림은 다시 보내지 않는다. 삭제 API는 없고 `useYn=false`로 비노출한다. 없으면 404 NOTICE_NOT_FOUND | ADM-040-04·05 | LJS | AdminNoticeUpdateRequest | AdminNoticeResponse |
| POST | /admin/notices/images | 공지 본문 이미지 올리기(multipart `image`, 이미지만 10MB, 공개 파일). 본문에는 `![](/api/files/public/{atchFileSn})` 한 줄로 넣는다(issues.md 고객센터 10, 2026-11-02 결정) | ADM-040-02 | LJS | - | AdminNoticeImageResponse |
| GET | /admin/inquiries | 관리자 1:1 문의 목록. 접수 최신순 20건씩. `inqryTypeCd`(CSCG02)·`inqryCtgr`(DISPUTE 분쟁·ERROR 오류·ETC 기타)·`inqryPrcsSttsCd`(CSCG03)·`bgngYmd`~`endYmd`(접수일, 끝 날짜 포함)는 AND. `inclDelYn=true`면 회원이 삭제한 문의도 나온다. 내부 분류는 유형으로 정한다(건강기록·픽셀 캐릭터=오류, 커뮤니티·포인트·결제·가계부=분쟁, 나머지=기타). 없는 코드·시작일이 끝일 뒤면 400 INVALID_INPUT_VALUE. 일반 회원 403 ADMIN_ONLY | ADM-042-01·02 | LJS | AdminInquirySearchCond | PageResponse<AdminInquirySummary> |
| GET | /admin/inquiries/{otoInqrySn} | 관리자 1:1 문의 상세(삭제 문의 포함, 상태 이력·첨부·답변·캐릭터 오류 처리 결과). 회원이 삭제한 문의는 첨부 목록이 비어 있다. 없으면 404 INQUIRY_NOT_FOUND | ADM-042-03·06·07·08 | LJS | - | AdminInquiryResponse |
| GET | /admin/inquiries/{otoInqrySn}/images/{atchFileSn} | 관리자 1:1 문의 첨부 이미지(이미지 바이너리). 그 문의의 삭제되지 않은 첨부나 접수 시점 캐릭터 이미지(`charImgAtchFileSn`)가 아니거나 삭제된 문의면 404 FILE_NOT_FOUND | ADM-042-03·07 | LJS | - | byte[] |
| POST | /admin/inquiries/{otoInqrySn}/answer | 답변 등록(201, 감사 로그). 상태를 답변 완료로 바꾸고 답변 일시·처리 관리자를 남기며 작성자에게 1:1 문의 답변 인앱 알림(AIMC0030)을 보낸다. 이미 답변했으면 409 ALREADY_PROCESSED, 삭제된 문의는 409 INQUIRY_DELETED | ADM-042-04·06 | LJS | AdminInquiryAnswerRequest | AdminInquiryResponse |
| PATCH | /admin/inquiries/{otoInqrySn}/answer | 답변 수정(감사 로그). 알림은 다시 보내지 않고 답변 일시·처리 관리자는 그대로다. 답변 전이면 400 INQUIRY_NOT_ANSWERED, 삭제된 문의는 409 INQUIRY_DELETED. 답변 삭제 API는 없다 | ADM-042-05·06 | LJS | AdminInquiryAnswerRequest | AdminInquiryResponse |
| POST | /admin/inquiries/{otoInqrySn}/character-error | 캐릭터 오류 처리(감사 로그). CHRC0008 무료 재생성 / CHRC0009 원래 캐릭터 복원을 기록하고 캐릭터 쪽 처리를 실행한다. 회원 알림은 캐릭터 쪽이 보낸다(복원은 바로, 재생성은 생성 완료 때). 답변 상태는 바꾸지 않는다. 캐릭터 오류 문의가 아니거나 없는 결과 코드면 400 INVALID_INPUT_VALUE, 이미 처리했으면 409 ALREADY_PROCESSED, 삭제된 문의는 409 INQUIRY_DELETED, 캐릭터 생성 중이면 409 PIXEL_CHARACTER_CREATING | ADM-042-07 | LJS | AdminCharacterErrorRequest | AdminInquiryResponse |

## DTO

### AdminCharacterErrorRequest
쓰는 API: POST /admin/inquiries/{otoInqrySn}/character-error
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| charErrPrcsRsltCd | String | Y | 공통코드 CHRG04. CHRC0008 무료 재생성, CHRC0009 원래 캐릭터 복원 | OTO_INQRY.CHAR_ERR_PRCS_RSLT_CD, ADM-042-07 |

### AdminInquiryAnswerRequest
쓰는 API: POST /admin/inquiries/{otoInqrySn}/answer, PATCH /admin/inquiries/{otoInqrySn}/answer
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ansCn | String | Y | 공백 불가, 4,000자 이하 | OTO_INQRY.ANS_CN, ADM-042-04·05 |

### AdminInquiryHistorySummary
쓰는 API: GET /admin/inquiries/{otoInqrySn} (응답 `AdminInquiryResponse.hstrys`, 오래된 순)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bfrSttsCd | String | N | 접수 이력은 null. 공통코드 CSCG03 | OTO_INQRY_STTS_HSTRY.BFR_STTS_CD |
| bfrSttsCdNm | String | N | | COM_CD.COM_CD_NM |
| aftrSttsCd | String | Y | 공통코드 CSCG03 | OTO_INQRY_STTS_HSTRY.AFTR_STTS_CD |
| aftrSttsCdNm | String | Y | | COM_CD.COM_CD_NM |
| mbrSn | Long | N | 바꾼 회원(접수는 작성자, 답변은 관리자) | OTO_INQRY_STTS_HSTRY.MBR_SN |
| mbrNcnm | String | N | | MBR.MBR_NCNM |
| chgRsn | String | N | | OTO_INQRY_STTS_HSTRY.CHG_RSN |
| regDt | LocalDateTime | Y | 바뀐 시각 | OTO_INQRY_STTS_HSTRY.REG_DT |

### AdminInquiryResponse
쓰는 API: GET /admin/inquiries/{otoInqrySn}, POST·PATCH /admin/inquiries/{otoInqrySn}/answer, POST /admin/inquiries/{otoInqrySn}/character-error
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD |
| inqryTypeCdNm | String | Y | | COM_CD.COM_CD_NM |
| inqryCtgr | String | Y | 내부 분류 DISPUTE·ERROR·ETC. DISPUTE면 [작성자 매칭 목록](SCR-ADM-022)을 `mbrSn`으로 연다 | ADM-042-01·08 |
| inqryPrcsSttsCd | String | Y | 공통코드 CSCG03 | OTO_INQRY.INQRY_PRCS_STTS_CD |
| inqryPrcsSttsCdNm | String | Y | | COM_CD.COM_CD_NM |
| inqryTtl | String | Y | | OTO_INQRY.INQRY_TTL |
| inqryCn | String | Y | | OTO_INQRY.INQRY_CN |
| mbrSn | Long | Y | 작성자. 회원 상세(SCR-ADM-011) 이동 | OTO_INQRY.MBR_SN, ADM-042-03 |
| mbrNcnm | String | N | | MBR.MBR_NCNM |
| prcsMbrSn | Long | N | 답변한 관리자, 답변 전 null | OTO_INQRY.PRCS_MBR_SN |
| prcsMbrNcnm | String | N | | MBR.MBR_NCNM |
| ansCn | String | N | 답변 전 null | OTO_INQRY.ANS_CN |
| ansDt | LocalDateTime | N | 답변 전 null | OTO_INQRY.ANS_DT |
| pixelCharSn | Long | N | 캐릭터 오류 문의만 | OTO_INQRY.PIXEL_CHAR_SN, ADM-042-07 |
| charErrPrcsRsltCd | String | N | 처리 전 null. 공통코드 CHRG04 | OTO_INQRY.CHAR_ERR_PRCS_RSLT_CD, ADM-042-07 |
| charErrPrcsRsltCdNm | String | N | | COM_CD.COM_CD_NM |
| charImgAtchFileSn | Long | N | 접수 시점 캐릭터 이미지. 문의의 캐릭터 사진(`CHAR_PHOTO_SN`) 파일, 없으면 캐릭터 오류 문의의 정렬 순서 0 첨부. 삭제된 문의는 null. 이미지는 관리자 첨부 이미지 API | CHAR_PHOTO.ATCH_FILE_SN(OTO_INQRY.CHAR_PHOTO_SN), INQRY_ATCH_PHOTO.ATCH_FILE_SN, ADM-042-07 |
| delYn | boolean | Y | true면 조회만 되고 답변 입력 칸을 숨긴다("회원이 삭제한 문의예요") | OTO_INQRY.DEL_YN, ADM-042-06 |
| regDt | LocalDateTime | Y | 접수일 | OTO_INQRY.REG_DT |
| imgs | List<InquiryImageSummary> | Y | 이미지는 `GET /api/admin/inquiries/{otoInqrySn}/images/{atchFileSn}`. 삭제된 문의는 빈 목록 | INQRY_ATCH_PHOTO, ADM-042-03 |
| hstrys | List<AdminInquiryHistorySummary> | Y | 상태 이력 | OTO_INQRY_STTS_HSTRY, ADM-042-03 |

### AdminInquirySearchCond
쓰는 API: GET /admin/inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inqryTypeCd | String | N | CSCG02 하위 코드, 없거나 공백이면 전체 | OTO_INQRY.INQRY_TYPE_CD, ADM-042-02 |
| inqryCtgr | String | N | DISPUTE·ERROR·ETC, 없거나 공백이면 전체 | ADM-042-02 |
| inqryPrcsSttsCd | String | N | CSCG03 하위 코드(CSCC0012 미답변, CSCC0013 답변 완료) | OTO_INQRY.INQRY_PRCS_STTS_CD, ADM-042-02 |
| bgngYmd | LocalDate | N | 접수일 시작 | OTO_INQRY.REG_DT, ADM-042-02 |
| endYmd | LocalDate | N | 접수일 끝(그날 포함) | OTO_INQRY.REG_DT, ADM-042-02 |
| inclDelYn | Boolean | N | true면 삭제 문의 포함, 기본 false | OTO_INQRY.DEL_YN, ADM-042-02 |

### AdminInquirySummary
쓰는 API: GET /admin/inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | | OTO_INQRY.INQRY_TYPE_CD, ADM-042-01 |
| inqryTypeCdNm | String | Y | | COM_CD.COM_CD_NM |
| inqryCtgr | String | Y | DISPUTE·ERROR·ETC | ADM-042-01 |
| inqryTtl | String | Y | | OTO_INQRY.INQRY_TTL, ADM-042-01 |
| mbrSn | Long | Y | 작성자 | OTO_INQRY.MBR_SN |
| mbrNcnm | String | N | | MBR.MBR_NCNM, ADM-042-01 |
| inqryPrcsSttsCd | String | Y | CSCC0012는 화면에서 '미답변' | OTO_INQRY.INQRY_PRCS_STTS_CD, ADM-042-01 |
| inqryPrcsSttsCdNm | String | Y | | COM_CD.COM_CD_NM |
| regDt | LocalDateTime | Y | 접수일 | OTO_INQRY.REG_DT, ADM-042-01 |
| delYn | boolean | Y | 회원이 삭제한 문의 | OTO_INQRY.DEL_YN, ADM-042-02 |

### AdminFaqCreateRequest
쓰는 API: POST /admin/faqs
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqClsfCd | String | Y | 공통코드 CSCG01 하위 코드, 없는 코드는 400 | FAQ.FAQ_CLSF_CD, ADM-041-02·04 |
| qstnTtl | String | Y | 공백 불가, 200자 이하 | FAQ.QSTN_TTL, ADM-041-02 |
| ansCn | String | Y | 공백 불가, 4,000자 이하 | FAQ.ANS_CN, ADM-041-02 |
| sortSeq | Integer | Y | 0 이상, 중복 허용 | FAQ.SORT_SEQ, ADM-041-02 |
| useYn | Boolean | Y | false면 비노출 | FAQ.USE_YN, ADM-041-02·03 |

### AdminFaqResponse
쓰는 API: GET /admin/faqs/{faqSn}, POST /admin/faqs, PATCH /admin/faqs/{faqSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqSn | Long | Y | | FAQ.FAQ_SN |
| faqClsfCd | String | Y | | FAQ.FAQ_CLSF_CD |
| qstnTtl | String | Y | | FAQ.QSTN_TTL |
| ansCn | String | Y | | FAQ.ANS_CN |
| sortSeq | Integer | Y | | FAQ.SORT_SEQ |
| useYn | boolean | Y | | FAQ.USE_YN |
| regDt | LocalDateTime | Y | | 공통 감사 컬럼 REG_DT |
| mdfcnDt | LocalDateTime | Y | | 공통 감사 컬럼 MDFCN_DT |

### AdminFaqSearchCond
쓰는 API: GET /admin/faqs
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqClsfCd | String | N | CSCG01 하위 코드, 없거나 공백이면 전체 | FAQ.FAQ_CLSF_CD, ADM-041-01·04 |
| keyword | String | N | 질문·답변 부분 일치, 없거나 공백이면 전체 | ADM-041-01 |

### AdminFaqSummary
쓰는 API: GET /admin/faqs
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqSn | Long | Y | | FAQ.FAQ_SN |
| faqClsfCd | String | Y | | FAQ.FAQ_CLSF_CD, ADM-041-01 |
| faqClsfCdNm | String | Y | 카테고리 이름 | COM_CD.COM_CD_NM, ADM-041-01 |
| qstnTtl | String | Y | | FAQ.QSTN_TTL, ADM-041-01 |
| sortSeq | Integer | Y | | FAQ.SORT_SEQ, ADM-041-01 |
| useYn | boolean | Y | | FAQ.USE_YN, ADM-041-01 |
| mdfcnDt | LocalDateTime | Y | 수정일 | 공통 감사 컬럼 MDFCN_DT |

### AdminFaqUpdateRequest
쓰는 API: PATCH /admin/faqs/{faqSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqClsfCd | String | N | 보내면 CSCG01 하위 코드 | FAQ.FAQ_CLSF_CD, ADM-041-02 |
| qstnTtl | String | N | 보내면 공백 불가, 200자 이하 | FAQ.QSTN_TTL, ADM-041-02 |
| ansCn | String | N | 보내면 공백 불가, 4,000자 이하 | FAQ.ANS_CN, ADM-041-02 |
| sortSeq | Integer | N | 보내면 0 이상 | FAQ.SORT_SEQ, ADM-041-02 |
| useYn | Boolean | N | false면 비노출(삭제 대신) | FAQ.USE_YN, ADM-041-03 |

### AdminNoticeCreateRequest
쓰는 API: POST /admin/notices
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrTtl | String | Y | 공백 불가, 200자 이하 | NTC_MTTR.NTC_MTTR_TTL, ADM-040-02 |
| ntcMttrCn | String | Y | 공백 불가, 4,000자 이하. 이미지는 `![](/api/files/public/{atchFileSn})` 줄 | NTC_MTTR.NTC_MTTR_CN, ADM-040-02 |
| upendFixYn | Boolean | Y | 상단 고정(개수 제한 없음) | NTC_MTTR.UPEND_FIX_YN, ADM-040-05 |
| useYn | Boolean | Y | false면 비노출 | NTC_MTTR.USE_YN, ADM-040-02 |
| notiSndngYn | Boolean | Y | 알림 발송. 등록 때만 받는다 | ADM-040-03 |

### AdminNoticeImageResponse
쓰는 API: POST /admin/notices/images
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| atchFileSn | Long | Y | 공개 이미지 `GET /api/files/public/{atchFileSn}` | ATCH_FILE.ATCH_FILE_SN, ADM-040-02 |

### AdminNoticeResponse
쓰는 API: GET /admin/notices/{ntcMttrSn}, POST /admin/notices, PATCH /admin/notices/{ntcMttrSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL |
| ntcMttrCn | String | Y | | NTC_MTTR.NTC_MTTR_CN |
| upendFixYn | boolean | Y | | NTC_MTTR.UPEND_FIX_YN |
| useYn | boolean | Y | | NTC_MTTR.USE_YN |
| inqCnt | Integer | Y | 조회수 | NTC_MTTR.INQ_CNT |
| notiSndngDt | LocalDateTime | N | 알림 발송 시각, null이면 미발송 | NTC_MTTR.NOTI_SNDNG_DT, ADM-040-03 |
| notiTrgtCnt | Integer | N | 알림 대상 수 | NTC_MTTR.NOTI_TRGT_CNT, ADM-040-03 |
| regDt | LocalDateTime | Y | | 공통 감사 컬럼 REG_DT |
| mdfcnDt | LocalDateTime | Y | | 공통 감사 컬럼 MDFCN_DT |

### AdminNoticeSearchCond
쓰는 API: GET /admin/notices
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| keyword | String | N | 제목 부분 일치, 없거나 공백이면 전체 | ADM-040-01 |

### AdminNoticeSummary
쓰는 API: GET /admin/notices
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL, ADM-040-01 |
| upendFixYn | boolean | Y | | NTC_MTTR.UPEND_FIX_YN, ADM-040-01 |
| useYn | boolean | Y | | NTC_MTTR.USE_YN, ADM-040-01 |
| inqCnt | Integer | Y | | NTC_MTTR.INQ_CNT, ADM-040-01 |
| notiSndngDt | LocalDateTime | N | null이면 미발송 | NTC_MTTR.NOTI_SNDNG_DT, ADM-040-01 |
| notiTrgtCnt | Integer | N | | NTC_MTTR.NOTI_TRGT_CNT, ADM-040-01 |
| regDt | LocalDateTime | Y | 등록일 | 공통 감사 컬럼 REG_DT, ADM-040-01 |

### AdminNoticeTargetCountResponse
쓰는 API: GET /admin/notices/notification-target-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trgtCnt | int | Y | 정상 회원(`MBR_STTS_CD='USRC0001'`) 수 | MBR.MBR_STTS_CD, ADM-040-03 |

### AdminNoticeUpdateRequest
쓰는 API: PATCH /admin/notices/{ntcMttrSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrTtl | String | N | 보내면 공백 불가, 200자 이하 | NTC_MTTR.NTC_MTTR_TTL, ADM-040-04 |
| ntcMttrCn | String | N | 보내면 공백 불가, 4,000자 이하 | NTC_MTTR.NTC_MTTR_CN, ADM-040-04 |
| upendFixYn | Boolean | N | | NTC_MTTR.UPEND_FIX_YN, ADM-040-05 |
| useYn | Boolean | N | false면 비노출(삭제 대신) | NTC_MTTR.USE_YN, ADM-040-04 |

### FaqSummary
쓰는 API: GET /faqs
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqSn | Long | Y | | FAQ.FAQ_SN |
| faqClsfCd | String | Y | 공통코드 CSCG01 | FAQ.FAQ_CLSF_CD, CSC-002-02 |
| faqClsfCdNm | String | Y | 카테고리 배지 | COM_CD.COM_CD_NM, CSC-002-01 |
| qstnTtl | String | Y | | FAQ.QSTN_TTL, CSC-002-01 |
| ansCn | String | Y | 아코디언 답변 | FAQ.ANS_CN, CSC-002-04 |

### InquiryCreateRequest
쓰는 API: POST /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inqryTypeCd | String | Y | 초안 그대로. 공통코드 CSCG02 단일 선택 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-06 |
| inqryTtl | String | Y | 최대 50자, 공백만은 불가(명세 v0.4, 2026-10-26 초안의 200자에서 변경) | OTO_INQRY.INQRY_TTL, CSC-003-08 |
| inqryCn | String | Y | 최대 2000자, 공백만은 불가(명세 v0.4, 2026-10-26 초안의 4000자에서 변경) | OTO_INQRY.INQRY_CN, CSC-003-08 |
| images | List<MultipartFile> | N | multipart/form-data로 함께 보낸다(2026-10-26 `atchFileSns`에서 변경: 파일을 먼저 올리는 공용 업로드 API가 없다). 최대 10개, 각 10MB 이내, JPG·PNG·WEBP | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-09 |
| pixelCharSn | Long | N | multipart. 캐릭터 오류 문의면 보낸다. 유형이 픽셀 캐릭터여야 하고, 저장 뒤 pet의 오류 접수가 실패하면(이미 접수·생성 중 등) 문의도 저장되지 않는다. 접수가 돌려준 캐릭터 이미지는 첨부 첫 번째로 붙고 첨부 합계 10개에 포함된다(사용자 이미지는 최대 9개) | OTO_INQRY.PIXEL_CHAR_SN (캐릭터 오류 문의), CSC-003-10 |
| charPhotoSn | Long | N | multipart. 초안 그대로. `pixelCharSn`과 함께 보낼 때만 받는다 | OTO_INQRY.CHAR_PHOTO_SN (캐릭터 오류 문의), CSC-003-10 |

### InquiryResponse
쓰는 API: GET /inquiries/{otoInqrySn}; PATCH /inquiries/{otoInqrySn}; POST /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | 초안 그대로 | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | 초안 그대로. 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD |
| inqryTypeCdNm | String | Y | 유형 표시 | COM_CD.COM_CD_NM, CSC-003-12 |
| inqryPrcsSttsCd | String | Y | 초안 그대로. 공통코드 CSCG03 | OTO_INQRY.INQRY_PRCS_STTS_CD |
| inqryPrcsSttsCdNm | String | Y | 상태 표시 | COM_CD.COM_CD_NM, CSC-003-12 |
| inqryTtl | String | Y | 초안 그대로 | OTO_INQRY.INQRY_TTL |
| inqryCn | String | Y | | OTO_INQRY.INQRY_CN, CSC-003-12 |
| imgs | List<InquiryImageSummary> | Y | 없으면 빈 목록 | INQRY_ATCH_PHOTO, CSC-003-12 |
| regDt | LocalDateTime | Y | 접수일 | 공통 감사 컬럼 REG_DT, CSC-003-12 |
| ansCn | String | N | 답변 전 null | OTO_INQRY.ANS_CN, CSC-003-13 |
| ansDt | LocalDateTime | N | 답변 전 null | OTO_INQRY.ANS_DT, CSC-003-13 |

### InquiryImageSummary
쓰는 API: GET /inquiries/{otoInqrySn}; PATCH /inquiries/{otoInqrySn}; POST /inquiries (응답 `InquiryResponse.imgs`. ImageItem에 문의 전용 `orgnlFileNm`을 더한 것)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| atchFileSn | Long | Y | 이미지는 `GET /api/inquiries/{otoInqrySn}/images/{atchFileSn}`(본인만) | INQRY_ATCH_PHOTO.ATCH_FILE_SN |
| sortSeq | Integer | Y | | INQRY_ATCH_PHOTO.SORT_SEQ |
| orgnlFileNm | String | Y | 첨부 팝업 표시용 원본 파일명 | ATCH_FILE.ORGNL_FILE_NM |

### InquirySummary
쓰는 API: GET /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-01 |
| inqryTypeCdNm | String | Y | 유형 표시 | COM_CD.COM_CD_NM, CSC-003-01 |
| inqryTtl | String | Y | | OTO_INQRY.INQRY_TTL, CSC-003-01 |
| inqryPrcsSttsCd | String | Y | 공통코드 CSCG03(접수·답변 완료) | OTO_INQRY.INQRY_PRCS_STTS_CD, CSC-003-01·02 |
| inqryPrcsSttsCdNm | String | Y | 상태 표시 | COM_CD.COM_CD_NM, CSC-003-01 |
| regDt | LocalDateTime | Y | 접수일 | 공통 감사 컬럼 REG_DT, CSC-003-01 |

### InquiryUpdateRequest
쓰는 API: PATCH /inquiries/{otoInqrySn}
접수 상태에서만 수정할 수 있다(CSC-003-15). 수정 가능한 범위는 issues 13.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inqryTypeCd | String | N | 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-06 |
| inqryTtl | String | N | 최대 50자(2026-10-26 200자에서 변경) | OTO_INQRY.INQRY_TTL, CSC-003-08·15 |
| inqryCn | String | N | 최대 2000자(2026-10-26 4000자에서 변경) | OTO_INQRY.INQRY_CN, CSC-003-08·15 |
| keepAtchFileSns | List<Long> | N | multipart. 남길 기존 첨부. 보낸 경우 여기에 없는 기존 첨부는 삭제한다(2026-10-26 `atchFileSns` 목록 교체에서 변경). 보내지 않으면 기존 첨부를 그대로 둔다 | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-15 |
| images | List<MultipartFile> | N | multipart. 새로 추가할 첨부. 남는 첨부와 합쳐 최대 10개, 각 10MB 이내 | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-09 |

### NoticeResponse
쓰는 API: GET /notices/{ntcMttrSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL, CSC-001-05 |
| ntcMttrCn | String | Y | 본문(텍스트 및 이미지, 이미지 담는 방식 issues 10) | NTC_MTTR.NTC_MTTR_CN, CSC-001-05 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CSC-001-05 |

### NoticeSummary
쓰는 API: GET /notices
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL, CSC-001-01 |
| upendFixYn | boolean | Y | 상단 고정 배경색 구분 | NTC_MTTR.UPEND_FIX_YN, CSC-001-02 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CSC-001-01·06 |
