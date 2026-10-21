
package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.TriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainRestorationTest {

    @Test
    void shouldRestoreWorkflowWithOriginalData() {
        Instant createdAt = Instant.parse("2026-10-21T10:00:00Z");
        Instant nextRun = Instant.parse("2026-10-21T11:00:00Z");

        Trigger trigger = Trigger.restore(
                TriggerType.CRON,
                3600,
                nextRun
        );

        Step step = Step.restore(
                "step-1",
                "Send message",
                1,
                ActionType.LOG_MESSAGE,
                Map.of("message", "Hello Aris"),
                2,
                1000
        );

        Workflow workflow = Workflow.restore(
                "workflow-1",
                "Daily automation",
                "Test workflow",
                trigger,
                false,
                createdAt,
                List.of(step)
        );

        assertAll(
                () -> assertEquals("workflow-1", workflow.getId()),
                () -> assertEquals(createdAt, workflow.getCreatedAt()),
                () -> assertEquals(nextRun, workflow.getTrigger().getNextRunTime()),
                () -> assertFalse(workflow.isEnabled()),
                () -> assertEquals("step-1", workflow.getSteps().getFirst().getId()),
                () -> assertEquals(1, workflow.getSteps().size())
        );
    }

    @Test
    void shouldRejectInvalidCronTrigger() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Trigger.restore(TriggerType.CRON, 0, null)
        );
    }

    @Test
    void shouldPreserveStepConfiguration() {
        Step step = Step.restore(
                "step-123",
                "Log",
                1,
                ActionType.LOG_MESSAGE,
                Map.of("message", "Test"),
                3,
                500
        );

        assertAll(
                () -> assertEquals("step-123", step.getId()),
                () -> assertEquals("Test", step.getConfig().get("message")),
                () -> assertEquals(3, step.getMaxRetries()),
                () -> assertEquals(500, step.getRetryBackoffMillis())
        );
    }
}
