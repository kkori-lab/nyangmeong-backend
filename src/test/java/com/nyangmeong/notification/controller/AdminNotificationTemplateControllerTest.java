package com.nyangmeong.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateResponse;
import com.nyangmeong.notification.dto.AdminNotificationTemplateSummary;
import com.nyangmeong.notification.service.AdminNotificationTemplateService;

/**
 * [검사 - 관리자 알림 템플릿 API]
 */
@WebMvcTest(controllers = AdminNotificationTemplateController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminNotificationTemplateControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String PATH = "/api/admin/notification-templates";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminNotificationTemplateService adminNotificationTemplateService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
        verify(adminNotificationTemplateService, never()).search(any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 목록은_채널로_거르고_사용_여부를_true_false로_준다() throws Exception {
        AdminNotificationTemplateSummary row = new AdminNotificationTemplateSummary();
        row.setNotiTmpltSn(3L);
        row.setNotiTypeCd("AIMC0008");
        row.setNotiChnlCd("AIMC0016");
        row.setNotiTtl("제목");
        row.setNotiCn("본문");
        row.setUseYn(true);
        when(adminNotificationTemplateService.search("AIMC0016")).thenReturn(List.of(row));

        mockMvc.perform(get(PATH).param("notiChnlCd", "AIMC0016"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notiTmpltSn").value(3))
                .andExpect(jsonPath("$.data[0].notiChnlCd").value("AIMC0016"))
                .andExpect(jsonPath("$.data[0].useYn").value(true));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 등록하면_201과_번호를_돌려준다() throws Exception {
        when(adminNotificationTemplateService.create(eq(9L), any()))
                .thenReturn(new AdminNotificationTemplateCreateResponse(7L));

        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notiTypeCd\":\"AIMC0008\",\"notiChnlCd\":\"AIMC0017\",\"notiTtl\":\"제목\","
                        + "\"notiCn\":\"{{반려동물명}} 일정\",\"useYn\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.notiTmpltSn").value(7));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 본문이_없으면_400() throws Exception {
        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notiTypeCd\":\"AIMC0008\",\"notiChnlCd\":\"AIMC0017\",\"notiCn\":\"\",\"useYn\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(adminNotificationTemplateService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 같은_유형_채널이면_409() throws Exception {
        when(adminNotificationTemplateService.create(eq(9L), any()))
                .thenThrow(new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_DUPLICATED));

        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notiTypeCd\":\"AIMC0008\",\"notiChnlCd\":\"AIMC0017\",\"notiCn\":\"본문\","
                        + "\"useYn\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_TEMPLATE_DUPLICATED"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_템플릿_수정은_404() throws Exception {
        doThrow(new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND))
                .when(adminNotificationTemplateService).update(eq(9L), eq(99L), any());

        mockMvc.perform(patch(PATH + "/99").header("X-Requested-With", "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notiTtl\":null,\"notiCn\":\"본문\",\"useYn\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_TEMPLATE_NOT_FOUND"));
    }
}
