package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.member.dto.AdminTermsCreateRequest;
import com.nyangmeong.member.dto.AdminTermsDetail;
import com.nyangmeong.member.mapper.TermsMapper;

/**
 * [검사 - 관리자 약관 목록·이력·상세·새 버전 등록]
 */
class AdminTermsServiceTest {

    private static final String KEY = "SERVICE";

    private final TermsMapper termsMapper = mock(TermsMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminTermsService service = new AdminTermsService(termsMapper, adminTaskLogService);

    private AdminTermsCreateRequest request(String verId) {
        return new AdminTermsCreateRequest(" " + KEY + " ", " " + verId + " ", " 서비스 이용약관 ", " 내용 ", true);
    }

    private AdminTermsDetail detail(Long trmsSn, String verId) {
        AdminTermsDetail detail = new AdminTermsDetail();
        detail.setTrmsSn(trmsSn);
        detail.setTrmsFixIdntfKey(KEY);
        detail.setTrmsVerId(verId);
        detail.setUseYn(true);
        return detail;
    }

    @Test
    void 새_버전_등록은_이전_버전을_끄고_정렬순서를_이어받는다() {
        when(termsMapper.selectLatestSortSeqForUpdate(KEY)).thenReturn(10);
        when(termsMapper.selectAdminDetailByVersion(KEY, "v1.1")).thenReturn(null, detail(9L, "v1.1"));

        AdminTermsDetail created = service.create(1L, request("v1.1"));

        assertThat(created.getTrmsSn()).isEqualTo(9L);
        InOrder order = inOrder(termsMapper);
        order.verify(termsMapper).selectLatestSortSeqForUpdate(KEY);
        order.verify(termsMapper).updateDisableByKey(KEY, "1");
        order.verify(termsMapper).insertAdminTerms(KEY, "v1.1", "서비스 이용약관", "내용", "Y", 10, "1");
        verify(adminTaskLogService).record(eq(1L), eq(AdminTaskType.CREATE), eq("TRMS"), eq(9L), anyString(),
                isNull(), eq(created));
    }

    @Test
    void 새_버전_등록은_기존_회원_동의를_건드리지_않는다() {
        when(termsMapper.selectLatestSortSeqForUpdate(KEY)).thenReturn(10);
        when(termsMapper.selectAdminDetailByVersion(KEY, "v1.1")).thenReturn(null, detail(9L, "v1.1"));

        service.create(1L, request("v1.1"));

        verify(termsMapper, never()).insertAgreement(anyLong(), anyLong(), anyString(), anyString());
        verify(termsMapper, never()).upsertAgreement(anyLong(), anyLong(), anyString());
    }

    @Test
    void 없는_종류면_404() {
        when(termsMapper.selectLatestSortSeqForUpdate("CARE_RULE")).thenReturn(null);

        assertThatThrownBy(() -> service.create(1L,
                new AdminTermsCreateRequest("CARE_RULE", "v1.0", "이용수칙", "내용", true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_NOT_FOUND);
        verify(termsMapper, never()).updateDisableByKey(anyString(), anyString());
    }

    @Test
    void 같은_종류_같은_버전이면_409() {
        when(termsMapper.selectLatestSortSeqForUpdate(KEY)).thenReturn(10);
        when(termsMapper.selectAdminDetailByVersion(KEY, "v1.0")).thenReturn(detail(1L, "v1.0"));

        assertThatThrownBy(() -> service.create(1L, request("v1.0")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TRMS_VER);
        verify(termsMapper, never()).updateDisableByKey(anyString(), anyString());
    }

    @Test
    void 동시에_같은_버전을_넣어_UK에_걸리면_409() {
        when(termsMapper.selectLatestSortSeqForUpdate(KEY)).thenReturn(10);
        doThrow(new DuplicateKeyException("UK_TRMS_1")).when(termsMapper)
                .insertAdminTerms(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString());

        assertThatThrownBy(() -> service.create(1L, request("v1.1")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_TRMS_VER);
    }

    @Test
    void 공백만인_버전은_400() {
        assertThatThrownBy(() -> service.create(1L, new AdminTermsCreateRequest(KEY, "   ", "제목", "내용", true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_약관_상세는_404() {
        when(termsMapper.selectAdminDetail(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_NOT_FOUND);
    }

    @Test
    void 이력은_종류_키가_없으면_400() {
        assertThatThrownBy(() -> service.searchHistory(" "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
