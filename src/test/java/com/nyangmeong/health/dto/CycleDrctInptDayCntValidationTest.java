package com.nyangmeong.health.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * [검사 - 주기 직접입력 일수 1~365]
 * - 케어 항목 등록·수정, 반복 예방 등록 요청
 */
class CycleDrctInptDayCntValidationTest {

    private static final String FIELD = "cycleDrctInptDayCnt";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private CareItemCreateRequest create(int dayCnt) {
        return new CareItemCreateRequest("항목", null, "HLTC0037", "HLTC0042", dayCnt, null);
    }

    private CareItemUpdateRequest update(int dayCnt) {
        return new CareItemUpdateRequest(null, null, null, "HLTC0042", dayCnt);
    }

    private RepeatPreventionCreateRequest prevention(int dayCnt) {
        return new RepeatPreventionCreateRequest(1L, "HLTC0001", "HLTC0042", dayCnt, LocalDate.of(2026, 11, 11));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 365})
    void 일수가_1_365면_통과(int dayCnt) {
        assertThat(invalidFields(create(dayCnt))).isEmpty();
        assertThat(invalidFields(update(dayCnt))).isEmpty();
        assertThat(invalidFields(prevention(dayCnt))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {366, 0, -1})
    void 일수가_1_365를_벗어나면_cycleDrctInptDayCnt_오류(int dayCnt) {
        assertThat(invalidFields(create(dayCnt))).containsExactly(FIELD);
        assertThat(invalidFields(update(dayCnt))).containsExactly(FIELD);
        assertThat(invalidFields(prevention(dayCnt))).containsExactly(FIELD);
    }
}
