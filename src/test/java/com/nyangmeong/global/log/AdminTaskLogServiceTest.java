package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 관리자 작업 로그 기록]
 */
class AdminTaskLogServiceTest {

    private final AdminTaskLogMapper adminTaskLogMapper = mock(AdminTaskLogMapper.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @AfterEach
    void clearMdc() {
        MdcUtils.clear();
    }

    @Test
    void 전후_값을_JSON으로_저장한다() {
        MdcUtils.put(MdcUtils.CLIENT_IP, "203.0.113.7");
        AdminTaskLogService service = new AdminTaskLogService(adminTaskLogMapper, objectMapper);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("petMaxCnt", 10L);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("petMaxCnt", 20L);

        service.record(7L, AdminTaskType.UPDATE, "SYS_STNG", 1L, "운영 설정 수정: petMaxCnt", before, after);

        ArgumentCaptor<AdminTaskLog> captor = ArgumentCaptor.forClass(AdminTaskLog.class);
        verify(adminTaskLogMapper).insert(captor.capture());
        AdminTaskLog saved = captor.getValue();
        assertThat(saved.getMbrSn()).isEqualTo(7L);
        assertThat(saved.getTaskTypeCd()).isEqualTo("SYSC0002");
        assertThat(saved.getTrgtTblNm()).isEqualTo("SYS_STNG");
        assertThat(saved.getTrgtSn()).isEqualTo(1L);
        assertThat(saved.getChgBfrCn()).isEqualTo("{\"petMaxCnt\":10}");
        assertThat(saved.getChgAftrCn()).isEqualTo("{\"petMaxCnt\":20}");
        assertThat(saved.getCntnIpAddr()).isEqualTo("203.0.113.7");
    }

    @Test
    void 전후_객체가_없으면_내용은_null() {
        AdminTaskLogService service = new AdminTaskLogService(adminTaskLogMapper, objectMapper);

        service.record(7L, AdminTaskType.LOGIN, "MBR", 7L, "관리자 로그인", null, null);

        ArgumentCaptor<AdminTaskLog> captor = ArgumentCaptor.forClass(AdminTaskLog.class);
        verify(adminTaskLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getChgBfrCn()).isNull();
        assertThat(captor.getValue().getChgAftrCn()).isNull();
        assertThat(captor.getValue().getCntnIpAddr()).isNull();
    }

    @Test
    void 트랜잭션_밖에서_호출하면_예외() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TxConfig.class)) {
            AdminTaskLogService service = context.getBean(AdminTaskLogService.class);
            AdminTaskLogMapper mapper = context.getBean(AdminTaskLogMapper.class);

            assertThatThrownBy(() -> service.record(7L, AdminTaskType.UPDATE, "SYS_STNG", 1L, "요약", null, null))
                    .isInstanceOf(IllegalTransactionStateException.class);
            verify(mapper, never()).insert(any());
        }
    }

    @Test
    void 트랜잭션_안에서는_기록한다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TxConfig.class)) {
            AdminTaskLogService service = context.getBean(AdminTaskLogService.class);
            AdminTaskLogMapper mapper = context.getBean(AdminTaskLogMapper.class);
            TransactionTemplate template = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

            template.executeWithoutResult(
                    status -> service.record(7L, AdminTaskType.UPDATE, "SYS_STNG", 1L, "요약", null, null));

            verify(mapper).insert(any());
        }
    }

    /**
     * 트랜잭션 규칙만 확인하는 최소 컨텍스트 (DB 없음)
     */
    @Configuration
    @EnableTransactionManagement
    static class TxConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoopTransactionManager();
        }

        @Bean
        AdminTaskLogMapper adminTaskLogMapper() {
            return mock(AdminTaskLogMapper.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AdminTaskLogService adminTaskLogService(AdminTaskLogMapper mapper, ObjectMapper objectMapper) {
            return new AdminTaskLogService(mapper, objectMapper);
        }
    }

    /**
     * 시작·커밋을 아무것도 하지 않는 트랜잭션 매니저
     */
    static class NoopTransactionManager extends AbstractPlatformTransactionManager {

        /** 진행 중 여부 (MANDATORY 판정용) */
        private boolean active;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            active = false;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            active = false;
        }
    }
}
