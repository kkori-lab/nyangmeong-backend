package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.domain.Terms;
import com.nyangmeong.member.dto.TermsAgreeRequest;
import com.nyangmeong.member.dto.TermsSummary;
import com.nyangmeong.member.mapper.TermsMapper;

/**
 * [검사 - 약관 서비스]
 * - 필수 약관 미동의·모르는 약관·중복 항목 거절, 선택 약관 누락은 미동의로 저장
 */
class TermsServiceTest {

    private final TermsMapper termsMapper = mock(TermsMapper.class);
    private final TermsService service = new TermsService(termsMapper);

    private TermsSummary summary(long trmsSn, boolean isEssential) {
        TermsSummary summary = new TermsSummary();
        summary.setTrmsSn(trmsSn);
        summary.setEsntlAgreYn(isEssential);
        return summary;
    }

    private void givenUsable() {
        when(termsMapper.selectUsableList())
                .thenReturn(List.of(summary(1, true), summary(2, true), summary(3, true), summary(4, false)));
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 필수_약관에_모두_동의하면_선택_약관_미동의도_N으로_정리된다() {
        givenUsable();

        Map<Long, Boolean> result = service.checkAgreements(List.of(new TermsAgreeRequest(1L, true),
                new TermsAgreeRequest(2L, true), new TermsAgreeRequest(3L, true), new TermsAgreeRequest(4L, false)));

        assertThat(result).containsEntry(1L, true).containsEntry(3L, true).containsEntry(4L, false);
    }

    @Test
    void 보내지_않은_선택_약관은_미동의로_본다() {
        givenUsable();

        Map<Long, Boolean> result = service.checkAgreements(
                List.of(new TermsAgreeRequest(1L, true), new TermsAgreeRequest(2L, true), new TermsAgreeRequest(3L, true)));

        assertThat(result).hasSize(4).containsEntry(4L, false);
    }

    @Test
    void 필수_약관에_동의하지_않으면_거절한다() {
        givenUsable();

        assertThatThrownBy(() -> service.checkAgreements(List.of(new TermsAgreeRequest(1L, true),
                new TermsAgreeRequest(2L, true), new TermsAgreeRequest(3L, false))))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
    }

    @Test
    void 필수_약관을_빠뜨려도_거절한다() {
        givenUsable();

        assertThatThrownBy(() -> service.checkAgreements(
                List.of(new TermsAgreeRequest(1L, true), new TermsAgreeRequest(2L, true))))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
    }

    @Test
    void 사용하지_않거나_없는_약관_번호는_거절한다() {
        givenUsable();

        assertThatThrownBy(() -> service.checkAgreements(List.of(new TermsAgreeRequest(99L, true))))
                .satisfies(e -> assertCode(e, ErrorCode.TERMS_NOT_FOUND));
    }

    @Test
    void 같은_약관을_두_번_보내면_거절한다() {
        givenUsable();

        assertThatThrownBy(() -> service.checkAgreements(
                List.of(new TermsAgreeRequest(1L, true), new TermsAgreeRequest(1L, true))))
                .satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
    }

    @Test
    void 동의_저장은_약관마다_Y_N으로_남긴다() {
        Map<Long, Boolean> agreements = new LinkedHashMap<>();
        agreements.put(1L, true);
        agreements.put(4L, false);

        service.createAgreements(9L, agreements);

        verify(termsMapper).insertAgreement(9L, 1L, "Y", "9");
        verify(termsMapper).insertAgreement(9L, 4L, "N", "9");
    }

    @Test
    void 없는_약관_전문은_404이다() {
        assertThatThrownBy(() -> service.getTerms(5L)).satisfies(e -> assertCode(e, ErrorCode.TERMS_NOT_FOUND));
    }

    @Test
    void 약관_전문을_돌려준다() {
        Terms terms = new Terms();
        terms.setTrmsSn(5L);
        terms.setTrmsCn("전문");
        terms.setEsntlAgreYn("Y");
        when(termsMapper.selectUsableBySn(5L)).thenReturn(terms);

        assertThat(service.getTerms(5L).trmsCn()).isEqualTo("전문");
        assertThat(service.getTerms(5L).esntlAgreYn()).isTrue();
    }

    @Test
    void 복구는_빠진_필수_약관에_모두_동의해야_하고_그_밖의_항목은_무시한다() {
        List<TermsSummary> missing = List.of(summary(2, true), summary(3, true));

        List<Long> saved = service.checkRecoveryAgreements(missing, List.of(new TermsAgreeRequest(2L, true),
                new TermsAgreeRequest(3L, true), new TermsAgreeRequest(4L, true), new TermsAgreeRequest(99L, true)));

        assertThat(saved).containsExactly(2L, 3L);
    }

    @Test
    void 복구는_빠진_필수_약관을_동의하지_않거나_보내지_않으면_거절한다() {
        List<TermsSummary> missing = List.of(summary(2, true), summary(3, true));

        assertThatThrownBy(() -> service.checkRecoveryAgreements(missing,
                List.of(new TermsAgreeRequest(2L, true), new TermsAgreeRequest(3L, false))))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        assertThatThrownBy(() -> service.checkRecoveryAgreements(missing, List.of(new TermsAgreeRequest(2L, true))))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        assertThatThrownBy(() -> service.checkRecoveryAgreements(missing, null))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
    }

    @Test
    void 복구는_빠진_필수_약관이_없으면_아무것도_보내지_않아도_된다() {
        assertThat(service.checkRecoveryAgreements(List.of(), null)).isEmpty();
        assertThat(service.checkRecoveryAgreements(List.of(), List.of())).isEmpty();
    }

    @Test
    void 복구_약관_조회는_회원이_동의하지_않은_필수_약관을_그대로_돌려준다() {
        when(termsMapper.selectMissingEssential(9L)).thenReturn(List.of(summary(2, true)));

        assertThat(service.searchMissingEssential(9L)).extracting(TermsSummary::getTrmsSn).containsExactly(2L);
    }

    @Test
    void 복구_동의_저장은_약관마다_동의로_남긴다() {
        service.createRecoveryAgreements(9L, List.of(2L, 3L));

        verify(termsMapper).upsertAgreement(9L, 2L, "9");
        verify(termsMapper).upsertAgreement(9L, 3L, "9");
    }
}
