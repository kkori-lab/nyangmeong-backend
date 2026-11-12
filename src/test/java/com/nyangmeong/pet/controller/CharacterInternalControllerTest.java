package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.file.StoredFile;
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
import com.nyangmeong.pet.dto.BakeItemSummary;
import com.nyangmeong.pet.dto.CharacterJobResponse;
import com.nyangmeong.pet.dto.CharacterSheetItem;
import com.nyangmeong.pet.dto.CharacterSheetsResponse;
import com.nyangmeong.pet.dto.CharacterSuccessResponse;
import com.nyangmeong.pet.service.CharacterCreationService;
import com.nyangmeong.pet.service.CharacterSheetService;
import com.nyangmeong.pet.service.PixelCharacterService;

/**
 * [검사 - 캐릭터 내부 API]
 */
@WebMvcTest(controllers = CharacterInternalController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CharacterInternalControllerTest {

    private static final String BASE = "/internal/ai/characters";
    private static final String REQUESTED_WITH = "X-Requested-With";
    private static final String XHR = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PixelCharacterService pixelCharacterService;

    @MockitoBean
    CharacterCreationService characterCreationService;

    @MockitoBean
    CharacterSheetService characterSheetService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MockMultipartFile image = new MockMultipartFile("image", "image.png", "image/png", new byte[] {1});
    private final MockMultipartFile sheet = new MockMultipartFile("sheet", "sheet.png", "image/png", new byte[] {2});

    private MockHttpServletRequestBuilder jsonPost(String path, String body) {
        return post(BASE + path).contentType(MediaType.APPLICATION_JSON).content(body).header(REQUESTED_WITH, XHR);
    }

    @Test
    void 쿠키가_없으면_8개_모두_401() throws Exception {
        mockMvc.perform(jsonPost("/jobs", "{\"petSn\":10}")).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(BASE + "/jobs/5/success").file(image).file(sheet).header(REQUESTED_WITH, XHR))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(jsonPost("/jobs/5/failure", "{\"failRsn\":\"CHARACTER_API_ERROR\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/bake-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/3/item-sheets/7").file(sheet).header(REQUESTED_WITH, XHR))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/3/item-sheets/7/failure").header(REQUESTED_WITH, XHR))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/3/sheets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/3/sheet-files/50")).andExpect(status().isUnauthorized());
        verifyNoInteractions(pixelCharacterService, characterCreationService, characterSheetService);
    }

    @Test
    @WithMockLoginMember
    void 작업_생성은_201과_반려동물_정보를_준다() throws Exception {
        when(pixelCharacterService.createJob(1L, 10L))
                .thenReturn(new CharacterJobResponse(5L, "PETC0001", "말티즈", "흰색", "순함"));

        mockMvc.perform(jsonPost("/jobs", "{\"petSn\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.charCrtJobSn").value(5))
                .andExpect(jsonPath("$.data.petSeCd").value("PETC0001"))
                .andExpect(jsonPath("$.data.vrtyNm").value("말티즈"))
                .andExpect(jsonPath("$.data.colrNm").value("흰색"))
                .andExpect(jsonPath("$.data.chrcCn").value("순함"));
    }

    @Test
    @WithMockLoginMember
    void 공동양육자의_작업_생성은_403() throws Exception {
        when(pixelCharacterService.createJob(1L, 10L)).thenThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY));

        mockMvc.perform(jsonPost("/jobs", "{\"petSn\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_PRIMARY_ONLY"));
    }

    @Test
    @WithMockLoginMember
    void 이미_있거나_생성_중이면_409() throws Exception {
        when(pixelCharacterService.createJob(1L, 10L))
                .thenThrow(new CustomException(ErrorCode.PIXEL_CHARACTER_ALREADY_EXISTS));
        when(pixelCharacterService.createJob(1L, 11L))
                .thenThrow(new CustomException(ErrorCode.PIXEL_CHARACTER_CREATING));

        mockMvc.perform(jsonPost("/jobs", "{\"petSn\":10}")).andExpect(status().isConflict());
        mockMvc.perform(jsonPost("/jobs", "{\"petSn\":11}")).andExpect(status().isConflict());
    }

    @Test
    @WithMockLoginMember
    void 성공_보고는_폼의_배치값을_받아_캐릭터_번호를_준다() throws Exception {
        when(characterCreationService.reportSuccess(eq(1L), eq(5L), any()))
                .thenReturn(new CharacterSuccessResponse(3L));

        mockMvc.perform(multipart(BASE + "/jobs/5/success").file(image).file(sheet)
                .param("scale", "1.5").param("originX", "2.0").param("originY", "3.0")
                .header(REQUESTED_WITH, XHR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pixelCharSn").value(3));
    }

    @Test
    @WithMockLoginMember
    void 배치값이_없으면_400() throws Exception {
        mockMvc.perform(multipart(BASE + "/jobs/5/success").file(image).file(sheet).header(REQUESTED_WITH, XHR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 실패_보고는_사유를_넘긴다() throws Exception {
        mockMvc.perform(jsonPost("/jobs/5/failure", "{\"failRsn\":\"CHARACTER_SPEC_MISMATCH\"}"))
                .andExpect(status().isOk());

        verify(characterCreationService).reportFailure(1L, 5L, "CHARACTER_SPEC_MISMATCH");
    }

    @Test
    @WithMockLoginMember
    void 굽기_대상_목록() throws Exception {
        BakeItemSummary item = new BakeItemSummary();
        item.setItemSn(7L);
        item.setRegnCd("MYPC0001");
        item.setBakeHint("a straw sun hat");
        when(characterSheetService.searchBakeItems()).thenReturn(List.of(item));

        mockMvc.perform(get(BASE + "/bake-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemSn").value(7))
                .andExpect(jsonPath("$.data[0].regnCd").value("MYPC0001"))
                .andExpect(jsonPath("$.data[0].bakeHint").value("a straw sun hat"));
    }

    @Test
    @WithMockLoginMember
    void 그림_참고_아이템은_굽기_지시_없이_그림_번호와_함께_나온다() throws Exception {
        BakeItemSummary item = new BakeItemSummary();
        item.setItemSn(8L);
        item.setRegnCd("MYPC0002");
        item.setBakeImgRefYn(true);
        item.setAtchFileSn(50L);
        when(characterSheetService.searchBakeItems()).thenReturn(List.of(item));

        mockMvc.perform(get(BASE + "/bake-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemSn").value(8))
                .andExpect(jsonPath("$.data[0].bakeHint").doesNotExist())
                .andExpect(jsonPath("$.data[0].bakeImgRefYn").value(true))
                .andExpect(jsonPath("$.data[0].atchFileSn").value(50));
    }

    @Test
    @WithMockLoginMember
    void 착용_시트는_PUT_multipart로_저장한다() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/3/item-sheets/7").file(sheet).header(REQUESTED_WITH, XHR))
                .andExpect(status().isOk());

        verify(characterSheetService).saveItemSheet(eq(1L), eq(false), eq(3L), eq(7L), any());
    }

    @Test
    @WithMockLoginMember
    void 굽기_실패_표시는_본문이_없다() throws Exception {
        mockMvc.perform(post(BASE + "/3/item-sheets/7/failure").header(REQUESTED_WITH, XHR))
                .andExpect(status().isOk());

        verify(characterSheetService).failItemSheet(1L, false, 3L, 7L);
    }

    @Test
    @WithMockLoginMember
    void 시트_정보는_반복된_itemSns를_받는다() throws Exception {
        when(characterSheetService.getSheets(1L, false, 3L, List.of(7L, 8L)))
                .thenReturn(new CharacterSheetsResponse("PETC0002", 50L, 1.5, 2.0, 3.0,
                        List.of(new CharacterSheetItem(7L, 60L, false), new CharacterSheetItem(8L, null, true))));

        mockMvc.perform(get(BASE + "/3/sheets").param("itemSns", "7").param("itemSns", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sheetFileSn").value(50))
                .andExpect(jsonPath("$.data.scale").value(1.5))
                .andExpect(jsonPath("$.data.originX").value(2.0))
                .andExpect(jsonPath("$.data.originY").value(3.0))
                .andExpect(jsonPath("$.data.items[0].atchFileSn").value(60))
                .andExpect(jsonPath("$.data.items[1].bakeFailed").value(true));
    }

    @Test
    @WithMockLoginMember
    void 시트_파일은_PNG_파일_자체다() throws Exception {
        when(characterSheetService.getSheetFile(1L, false, 3L, 50L))
                .thenReturn(new StoredFile(new byte[] {9}, "image/png", "sheet.png"));

        mockMvc.perform(get(BASE + "/3/sheet-files/50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {9}));
    }

    @Test
    @WithMockLoginMember
    void 남의_시트_파일_번호는_404() throws Exception {
        when(characterSheetService.getSheetFile(1L, false, 3L, 99L)).thenThrow(new CustomException(ErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get(BASE + "/3/sheet-files/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember
    void 대표_이미지_저장은_반복된_itemSns를_받는다() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/3/equipped-image").file(image)
                .param("itemSns", "7").param("itemSns", "8").header(REQUESTED_WITH, XHR))
                .andExpect(status().isOk());

        verify(pixelCharacterService).saveEquippedImage(eq(1L), eq(3L),
                argThat(req -> List.of(7L, 8L).equals(req.itemSns())));
    }

    @Test
    void 대표_이미지_저장도_쿠키가_없으면_401() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/3/equipped-image").file(image).header(REQUESTED_WITH, XHR))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pixelCharacterService);
    }

    @Test
    @WithMockLoginMember
    void 착용이_바뀌었으면_409_EQUIPPED_ITEMS_CHANGED() throws Exception {
        doThrow(new CustomException(ErrorCode.EQUIPPED_ITEMS_CHANGED)).when(pixelCharacterService)
                .saveEquippedImage(eq(1L), eq(3L), any());

        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/3/equipped-image").file(image).header(REQUESTED_WITH, XHR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPPED_ITEMS_CHANGED"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = "USRC0005")
    void 관리자_쿠키면_관리자로_넘긴다() throws Exception {
        mockMvc.perform(get(BASE + "/3/sheets"));
        mockMvc.perform(post(BASE + "/3/item-sheets/7/failure").header(REQUESTED_WITH, XHR))
                .andExpect(status().isOk());

        verify(characterSheetService).getSheets(9L, true, 3L, null);
        verify(characterSheetService).failItemSheet(9L, true, 3L, 7L);
    }
}
