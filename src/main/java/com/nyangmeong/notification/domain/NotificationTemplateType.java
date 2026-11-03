package com.nyangmeong.notification.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.global.notification.NotificationType;

/**
 * [템플릿을 쓰는 알림 유형과 치환 변수]
 * - 발송 코드가 NOTI_TMPLT를 읽는 유형만 : 나머지 유형은 기본 문구 (FN-ADM-090-03)
 * - 변수 표기는 {{이름}}
 */
public enum NotificationTemplateType {
    CARE_SCHEDULE(NotificationType.CARE_SCHEDULE.getCode(),
            List.of("닉네임", "반려동물명", "검진유형", "병원명", "일정일", "시간", "D-N", "캘린더링크"));

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([^{}]*)\\}\\}");

    private final String code;
    private final List<String> variables;

    NotificationTemplateType(String code, List<String> variables) {
        this.code = code;
        this.variables = variables;
    }

    public String getCode() {
        return code;
    }

    public List<String> getVariables() {
        return variables;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 알림 유형 코드로 조회
     */
    public static Optional<NotificationTemplateType> from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.is(code))
                .findFirst();
    }

    /**
     * 문구 안의 모르는 변수 목록
     */
    public Set<String> unknownVariables(String text) {
        Set<String> unknown = new LinkedHashSet<>();
        if (text == null) {
            return unknown;
        }
        Matcher matcher = VARIABLE.matcher(text);
        while (matcher.find()) {
            if (!variables.contains(matcher.group(1))) {
                unknown.add(matcher.group(1));
            }
        }
        return unknown;
    }
}
