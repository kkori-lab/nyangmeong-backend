package com.nyangmeong.global.aitool;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.nyangmeong.global.aitool.AiToolResponse.Code;
import com.nyangmeong.global.aitool.AiToolResponse.Param;
import com.nyangmeong.global.aitool.AiToolResponse.RequiredIf;
import com.nyangmeong.global.aitool.AiToolResponse.Rule;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.crypto.TokenHashUtil;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * [AI 매니저 도구 목록]
 * - @AiTool 컨트롤러 메서드의 매핑·인자·요청 DTO 검증 어노테이션 -> 도구 목록
 * - 쿼리 칸 : @RequestParam, @ModelAttribute 검색 조건 필드, PageQuery(page·size)
 * - 본문 칸 : @RequestBody record, multipart @ModelAttribute record(MultipartFile -> file)
 * - List<record> 칸 -> array + items(같은 칸 모양)
 * - 지원하지 않는 칸(값 목록·중첩 객체 등)이 있는 도구는 빼고 WARN 로그
 */
@Slf4j
@Service
public class AiToolCatalogService {

    private static final String PATH = "path";
    private static final String QUERY = "query";
    private static final String BODY = "body";
    private static final String CODE = "code";
    private static final String ARRAY = "array";
    private static final String FILE = "file";
    /** YearMonth 칸은 string + yyyy-MM 형식으로 싣는다 */
    private static final String YEAR_MONTH_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";

    /** 해시용 : 키 순서 고정 */
    private static final JsonMapper HASH_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private final RequestMappingHandlerMapping handlerMapping;
    private final CommonCodeService commonCodeService;
    private final TokenHashUtil tokenHashUtil;

    /** 코드 값 제외 도구 목록 : 매핑은 기동 뒤 바뀌지 않음 */
    private volatile List<AiToolResponse> structure;

    public AiToolCatalogService(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            CommonCodeService commonCodeService, TokenHashUtil tokenHashUtil) {
        this.handlerMapping = handlerMapping;
        this.commonCodeService = commonCodeService;
        this.tokenHashUtil = tokenHashUtil;
    }

    /**
     * 도구 목록 조회
     */
    @Transactional(readOnly = true)
    public AiToolCatalogResponse getCatalog() {
        List<AiToolResponse> structureTools = structure();
        Map<String, List<Code>> codesByGroup = new HashMap<>();
        List<AiToolResponse> tools = structureTools.stream().map(tool -> withCodes(tool, codesByGroup)).toList();
        return new AiToolCatalogResponse(hash(tools), hash(structureTools), tools);
    }

    private List<AiToolResponse> structure() {
        List<AiToolResponse> cached = structure;
        if (cached == null) {
            cached = collect();
            structure = cached;
        }
        return cached;
    }

    private List<AiToolResponse> collect() {
        List<AiToolResponse> tools = new ArrayList<>();
        Set<String> names = new HashSet<>();
        handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().hasMethodAnnotation(AiTool.class))
                .sorted(Comparator.comparing(entry -> entry.getValue().getMethodAnnotation(AiTool.class).name()))
                .forEach(entry -> {
                    AiTool aiTool = entry.getValue().getMethodAnnotation(AiTool.class);
                    if (!names.add(aiTool.name())) {
                        log.warn("[AiTool] 제외 name={} reason=이름 중복", aiTool.name());
                        return;
                    }
                    ToolDraft draft = toTool(aiTool, entry.getKey(), entry.getValue());
                    if (draft.skipReason() != null) {
                        log.warn("[AiTool] 제외 name={} reason={}", aiTool.name(), draft.skipReason());
                        return;
                    }
                    tools.add(draft.tool());
                });
        return List.copyOf(tools);
    }

    private ToolDraft toTool(AiTool aiTool, RequestMappingInfo info, HandlerMethod handlerMethod) {
        if (info.getMethodsCondition().getMethods().size() != 1 || info.getPatternValues().size() != 1) {
            return ToolDraft.skip("HTTP 메서드·경로가 하나가 아님");
        }
        String httpMethod = info.getMethodsCondition().getMethods().iterator().next().name();
        String path = info.getPatternValues().iterator().next();
        boolean isMultipart = info.getConsumesCondition().getConsumableMediaTypes().stream()
                .anyMatch(MediaType.MULTIPART_FORM_DATA::includes);

        Map<String, Param> params = new LinkedHashMap<>();
        List<Rule> rules = new ArrayList<>();
        Set<String> pathNames = new HashSet<>();
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
            if (pathVariable != null) {
                pathNames.add(nameOf(pathVariable.name(), parameter));
            }
        }
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            String skipReason = addParameter(parameter, params, rules, pathNames, isMultipart);
            if (skipReason != null) {
                return ToolDraft.skip(skipReason);
            }
        }
        for (Rule rule : rules) {
            String skipReason = checkRule(rule, params);
            if (skipReason != null) {
                return ToolDraft.skip(skipReason);
            }
        }
        return new ToolDraft(new AiToolResponse(aiTool.name(), aiTool.domain(), aiTool.kind(),
                new AiToolResponse.Call(httpMethod, path, isMultipart ? MediaType.MULTIPART_FORM_DATA_VALUE : null),
                params, List.copyOf(rules)), null);
    }

    /**
     * 컨트롤러 인자 하나 반영
     */
    private String addParameter(MethodParameter parameter, Map<String, Param> params, List<Rule> rules,
            Set<String> pathNames, boolean isMultipart) {
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            String name = nameOf(pathVariable.name(), parameter);
            return put(params, name, toParam(PATH, parameter.getParameterType(), annotated(parameter), true));
        }
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            String name = nameOf(requestParam.name(), parameter);
            boolean required = requestParam.required()
                    && ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue());
            return put(params, name, toParam(QUERY, parameter.getParameterType(), annotated(parameter), required));
        }
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return addBody(parameter.getParameterType(), params, rules, pathNames);
        }
        if (parameter.hasParameterAnnotation(ModelAttribute.class) && isMultipart) {
            return addBody(parameter.getParameterType(), params, rules, pathNames);
        }
        if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
            return addModelAttribute(parameter.getParameterType(), params, pathNames);
        }
        if (parameter.getParameterType() == PageQuery.class) {
            return addPageQuery(params);
        }
        /** 로그인 회원 등 요청 칸이 아닌 인자 */
        return null;
    }

    /**
     * 검색 조건 필드 -> 쿼리 칸
     * - 경로 변수와 이름이 같은 필드는 서비스가 경로 값으로 덮어쓰므로 뺌
     */
    private String addModelAttribute(Class<?> condType, Map<String, Param> params, Set<String> pathNames) {
        for (Field field : queryFieldsOf(condType)) {
            if (pathNames.contains(field.getName())) {
                continue;
            }
            String skipReason = put(params, field.getName(), toParam(QUERY, field.getType(), field, false));
            if (skipReason != null) {
                return skipReason;
            }
        }
        return null;
    }

    private static List<Field> queryFieldsOf(Class<?> condType) {
        List<Field> fields = new ArrayList<>();
        if (condType.isRecord()) {
            for (RecordComponent component : condType.getRecordComponents()) {
                try {
                    fields.add(condType.getDeclaredField(component.getName()));
                } catch (NoSuchFieldException e) {
                    /** record 칸은 같은 이름의 필드가 항상 있음 */
                    throw new IllegalStateException("record 필드를 찾을 수 없습니다: " + component.getName(), e);
                }
            }
            return fields;
        }
        for (Class<?> type = condType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    /**
     * 페이지 조건 -> page·size 쿼리 칸 (sort는 싣지 않음)
     */
    private String addPageQuery(Map<String, Param> params) {
        String skipReason = put(params, "page", new ParamDraft(new Param(QUERY, "int", null, null, null, null,
                (long) PageQuery.DEFAULT_PAGE, null, null, null, null, null, null, null, null, null), null));
        if (skipReason != null) {
            return skipReason;
        }
        return put(params, "size", new ParamDraft(new Param(QUERY, "int", null, null, null, null, 1L,
                (long) PageQuery.MAX_SIZE, null, null, null, null, null, null, null, null), null));
    }

    /**
     * 요청 본문 record -> 본문 칸
     * - 경로 변수와 이름이 같은 칸은 뺌(예: 일정 수정의 옮길 반려동물 petSn)
     */
    private String addBody(Class<?> bodyType, Map<String, Param> params, List<Rule> rules, Set<String> pathNames) {
        if (!bodyType.isRecord()) {
            return "요청 본문이 record가 아님: " + bodyType.getSimpleName();
        }
        for (Field field : queryFieldsOf(bodyType)) {
            if (pathNames.contains(field.getName())) {
                log.info("[AiTool] 경로 변수와 같은 본문 칸 뺌 body={} field={}", bodyType.getSimpleName(),
                        field.getName());
                continue;
            }
            String skipReason = put(params, field.getName(), toFieldParam(BODY, field));
            if (skipReason != null) {
                return skipReason;
            }
        }
        for (AiRequiredIf requiredIf : bodyType.getAnnotationsByType(AiRequiredIf.class)) {
            rules.add(new Rule(new RequiredIf(requiredIf.field(), requiredIf.when(), requiredIf.equals()), null));
        }
        for (AiOneOf oneOf : bodyType.getAnnotationsByType(AiOneOf.class)) {
            rules.add(new Rule(null, List.of(oneOf.value())));
        }
        return null;
    }

    private String put(Map<String, Param> params, String name, ParamDraft draft) {
        if (draft.skipReason() != null) {
            return name + " " + draft.skipReason();
        }
        if (params.putIfAbsent(name, draft.param()) != null) {
            return name + " 칸 이름 중복";
        }
        return null;
    }

    private String checkRule(Rule rule, Map<String, Param> params) {
        List<String> fields = rule.requiredIf() != null
                ? List.of(rule.requiredIf().field(), rule.requiredIf().when())
                : rule.oneOf();
        for (String field : fields) {
            if (!params.containsKey(field)) {
                return "규칙의 칸이 없음: " + field;
            }
        }
        return null;
    }

    /**
     * 본문 필드 -> 칸 : List<record>는 array + items, 그 밖은 toParam
     */
    private ParamDraft toFieldParam(String location, Field field) {
        if (field.getType() != List.class) {
            return toParam(location, field.getType(), field, false);
        }
        Class<?> itemType = listItemTypeOf(field.getGenericType());
        if (itemType == null || !itemType.isRecord()) {
            return ParamDraft.skip("타입 미지원: " + field.getGenericType().getTypeName());
        }
        Map<String, Param> items = new LinkedHashMap<>();
        for (Field itemField : queryFieldsOf(itemType)) {
            String skipReason = put(items, itemField.getName(), toFieldParam(null, itemField));
            if (skipReason != null) {
                return ParamDraft.skip("[]." + skipReason);
            }
        }
        NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
        Size size = field.getAnnotation(Size.class);
        boolean isRequired = notEmpty != null || field.isAnnotationPresent(NotNull.class);
        Integer minItems = notEmpty != null ? Integer.valueOf(1) : null;
        if (size != null && size.min() > 0) {
            minItems = minItems == null ? size.min() : Math.max(minItems, size.min());
        }
        return new ParamDraft(new Param(location, ARRAY, null, isRequired ? Boolean.TRUE : null, null, null, null,
                null, null, null, null, null, null, minItems, null, items.isEmpty() ? null : items),
                null);
    }

    private static Class<?> listItemTypeOf(Type genericType) {
        if (genericType instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> itemType) {
            return itemType;
        }
        return null;
    }

    /**
     * 칸 타입·검증 어노테이션 -> 칸
     */
    private ParamDraft toParam(String location, Class<?> type, AnnotatedElement element, boolean isPathOrRequired) {
        AiCode aiCode = element.getAnnotation(AiCode.class);
        String typeName = typeNameOf(type);
        if (typeName == null) {
            return ParamDraft.skip("타입 미지원: " + type.getSimpleName());
        }
        if (FILE.equals(typeName) && !BODY.equals(location)) {
            return ParamDraft.skip("파일 칸은 multipart 본문에만");
        }
        AiGenerated generated = element.getAnnotation(AiGenerated.class);
        if (aiCode != null) {
            if (type != String.class) {
                return ParamDraft.skip("@AiCode는 String 칸에만");
            }
            typeName = CODE;
        }
        /** 실행하는 쪽이 만드는 값은 LLM이 채우지 않게 필수로 싣지 않음 */
        boolean isRequired = generated == null && (isPathOrRequired || element.isAnnotationPresent(NotNull.class)
                || element.isAnnotationPresent(NotBlank.class) || element.isAnnotationPresent(NotEmpty.class));

        Size size = element.getAnnotation(Size.class);
        Min min = element.getAnnotation(Min.class);
        Max max = element.getAnnotation(Max.class);
        Pattern pattern = element.getAnnotation(Pattern.class);
        Digits digits = element.getAnnotation(Digits.class);
        return new ParamDraft(new Param(location, typeName,
                aiCode != null ? aiCode.value() : null,
                isRequired ? Boolean.TRUE : null,
                size != null && size.min() > 0 ? size.min() : null,
                size != null && size.max() != Integer.MAX_VALUE ? size.max() : null,
                min != null ? min.value() : null,
                max != null ? max.value() : null,
                element.isAnnotationPresent(Positive.class) ? Boolean.TRUE : null,
                pattern != null ? pattern.regexp() : type == YearMonth.class ? YEAR_MONTH_PATTERN : null,
                digits != null ? digits.integer() + digits.fraction() : null,
                digits != null ? digits.fraction() : null,
                null, null, generated != null ? generated.value() : null, null), null);
    }

    private static String typeNameOf(Class<?> type) {
        if (type == String.class || type == YearMonth.class) {
            return "string";
        }
        if (type == Long.class || type == long.class) {
            return "long";
        }
        if (type == Integer.class || type == int.class) {
            return "int";
        }
        if (type == BigDecimal.class) {
            return "decimal";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        }
        if (type == LocalDate.class) {
            return "date";
        }
        if (type == LocalTime.class) {
            return "time";
        }
        if (type == LocalDateTime.class) {
            return "datetime";
        }
        if (type == MultipartFile.class) {
            return FILE;
        }
        return null;
    }

    private static String nameOf(String declaredName, MethodParameter parameter) {
        return StringUtils.hasText(declaredName) ? declaredName : parameter.getParameterName();
    }

    private static AnnotatedElement annotated(MethodParameter parameter) {
        return parameter.getParameter();
    }

    private AiToolResponse withCodes(AiToolResponse tool, Map<String, List<Code>> codesByGroup) {
        return new AiToolResponse(tool.name(), tool.domain(), tool.kind(), tool.call(),
                withCodes(tool.params(), codesByGroup), tool.rules());
    }

    private Map<String, Param> withCodes(Map<String, Param> params, Map<String, List<Code>> codesByGroup) {
        Map<String, Param> result = new LinkedHashMap<>();
        params.forEach((name, param) -> {
            Param withItems = param.items() == null ? param : param.withItems(withCodes(param.items(), codesByGroup));
            result.put(name, withItems.group() == null ? withItems
                    : withItems.withCodes(codesByGroup.computeIfAbsent(withItems.group(), this::codesOf)));
        });
        return result;
    }

    private List<Code> codesOf(String group) {
        return commonCodeService.search(group).stream()
                .map(code -> new Code(code.getComCd(), code.getComCdNm()))
                .toList();
    }

    private String hash(List<AiToolResponse> tools) {
        return tokenHashUtil.hash(HASH_MAPPER.writeValueAsString(tools));
    }

    /** 도구 또는 제외 이유 */
    private record ToolDraft(AiToolResponse tool, String skipReason) {
        static ToolDraft skip(String reason) {
            return new ToolDraft(null, reason);
        }
    }

    /** 칸 또는 제외 이유 */
    private record ParamDraft(Param param, String skipReason) {
        static ParamDraft skip(String reason) {
            return new ParamDraft(null, reason);
        }
    }
}
