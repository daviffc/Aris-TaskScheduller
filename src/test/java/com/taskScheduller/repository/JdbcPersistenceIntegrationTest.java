
package com.taskScheduller.repository;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.ExecutionLog;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;
import com.taskScheduller.domain.enums.TriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JdbcPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("aris_test")
                    .withUsername("aris_test")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM execution_logs");
        jdbcTemplate.update("DELETE FROM executions");
        jdbcTemplate.update("DELETE FROM workflow_steps");
        jdbcTemplate.update("DELETE FROM workflows");
    }

    @Test
    void shouldPersistAndRestoreWorkflowWithSteps() {
        Instant createdAt = Instant.parse("2026-10-20T12:00:00Z");
        Instant nextRunTime = Instant.parse("2026-10-21T12:00:00Z");

        Step step = Step.restore(
                "step-integration-1",
                "Registrar mensagem",
                1,
                ActionType.LOG_MESSAGE,
                Map.of("message", "Testcontainers funcionando"),
                2,
                1000
        );

        Workflow workflow = Workflow.restore(
                "workflow-integration-1",
                "Workflow integrado",
                "Teste real de persistencia",
                Trigger.restore(TriggerType.CRON, 3600, nextRunTime),
                true,
                createdAt,
                List.of(step)
        );

        workflowRepository.save(workflow);

        Workflow restored = workflowRepository
                .findById(workflow.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(workflow.getId(), restored.getId()),
                () -> assertEquals("Workflow integrado", restored.getName()),
                () -> assertEquals(createdAt, restored.getCreatedAt()),
                () -> assertEquals(nextRunTime, restored.getTrigger().getNextRunTime()),
                () -> assertEquals(TriggerType.CRON, restored.getTrigger().getType()),
                () -> assertEquals(1, restored.getSteps().size()),
                () -> assertEquals(step.getId(), restored.getSteps().getFirst().getId()),
                () -> assertEquals(
                        "Testcontainers funcionando",
                        restored.getSteps().getFirst().getConfig().get("message")
                ),
                () -> assertEquals(
                        2,
                        restored.getSteps().getFirst().getMaxRetries()
                )
        );

        restored.setName("Workflow atualizado");
        restored.setDescription("Descricao atualizada");

        workflowRepository.save(restored);

        Workflow updated = workflowRepository
                .findById(workflow.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals("Workflow atualizado", updated.getName()),
                () -> assertEquals("Descricao atualizada", updated.getDescription()),
                () -> assertEquals(createdAt, updated.getCreatedAt()),
                () -> assertEquals(1, updated.getSteps().size()),
                () -> assertEquals(1, workflowRepository.findAll().size())
        );
    }

    @Test
    void shouldPersistExecutionStatusAndLogs() {
        Workflow workflow = new Workflow(
                "Workflow de execucao",
                "Teste de historico",
                Trigger.manual()
        );

        workflowRepository.save(workflow);

        Execution execution = new Execution(workflow.getId());

        executionRepository.save(execution);

        Execution pending = executionRepository
                .findById(execution.getId())
                .orElseThrow();

        assertEquals(ExecutionStatus.PENDING, pending.getStatus());
        assertNull(pending.getEndTime());

        execution.markRunning();
        executionRepository.save(execution);

        Execution running = executionRepository
                .findById(execution.getId())
                .orElseThrow();

        assertEquals(ExecutionStatus.RUNNING, running.getStatus());

        Instant logTime = Instant.parse("2026-10-21T12:30:00Z");

        execution.addLog(ExecutionLog.restore(
                "step-integration-2",
                "Executar acao",
                StepStatus.SUCCEEDED,
                "Etapa concluida",
                1,
                logTime
        ));

        execution.markSucceeded();
        executionRepository.save(execution);

        Execution restored = executionRepository
                .findById(execution.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(execution.getId(), restored.getId()),
                () -> assertEquals(workflow.getId(), restored.getWorkflowId()),
                () -> assertEquals(ExecutionStatus.SUCCEEDED, restored.getStatus()),
                () -> assertNotNull(restored.getEndTime()),
                () -> assertEquals(1, restored.getLogs().size()),
                () -> assertEquals(
                        "Executar acao",
                        restored.getLogs().getFirst().getStepName()
                ),
                () -> assertEquals(
                        StepStatus.SUCCEEDED,
                        restored.getLogs().getFirst().getStatus()
                ),
                () -> assertEquals(
                        logTime,
                        restored.getLogs().getFirst().getTimestamp()
                ),
                () -> assertEquals(
                        1,
                        executionRepository.findByWorkflowId(workflow.getId()).size()
                )
        );
    }

    @Test
    void shouldRollbackWhenStepInsertionFails() {
        Workflow workflow = new Workflow(
                "Workflow com erro",
                "Teste de rollback",
                Trigger.manual()
        );

        workflow.addStep(new Step(
                "Primeira etapa",
                1,
                ActionType.LOG_MESSAGE,
                Map.of("message", "Primeira"),
                0,
                0
        ));

        workflow.addStep(new Step(
                "Segunda etapa",
                1,
                ActionType.LOG_MESSAGE,
                Map.of("message", "Segunda"),
                0,
                0
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> workflowRepository.save(workflow)
        );

        assertTrue(workflowRepository.findById(workflow.getId()).isEmpty());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_steps WHERE workflow_id = ?",
                Integer.class,
                workflow.getId()
        );

        assertEquals(0, count);
    }
}
