
package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRestorationTest {

    @Test
    void shouldRestoreExecutionWithOriginalLogs() {
        Instant startTime = Instant.parse(
                "2026-10-21T12:00:00Z"
        );

        Instant endTime = Instant.parse(
                "2026-10-21T12:00:05Z"
        );

        Instant logTime = Instant.parse(
                "2026-10-21T12:00:03Z"
        );

        ExecutionLog log = ExecutionLog.restore(
                "step-1",
                "Send message",
                StepStatus.SUCCEEDED,
                "Completed",
                2,
                logTime
        );

        Execution execution = Execution.restore(
                "execution-1",
                "workflow-1",
                ExecutionStatus.SUCCEEDED,
                startTime,
                endTime,
                List.of(log)
        );

        assertAll(
                () -> assertEquals("execution-1", execution.getId()),
                () -> assertEquals("workflow-1", execution.getWorkflowId()),
                () -> assertEquals(
                        ExecutionStatus.SUCCEEDED,
                        execution.getStatus()
                ),
                () -> assertEquals(startTime, execution.getStartTime()),
                () -> assertEquals(endTime, execution.getEndTime()),
                () -> assertEquals(1, execution.getLogs().size()),
                () -> assertEquals(
                        "step-1",
                        execution.getLogs().getFirst().getStepId()
                ),
                () -> assertEquals(
                        2,
                        execution.getLogs().getFirst().getAttemptNumber()
                ),
                () -> assertEquals(
                        logTime,
                        execution.getLogs().getFirst().getTimestamp()
                )
        );
    }

    @Test
    void shouldCreateNewExecutionAsPending() {
        Execution execution = new Execution("workflow-2");

        assertAll(
                () -> assertNotNull(execution.getId()),
                () -> assertEquals(
                        ExecutionStatus.PENDING,
                        execution.getStatus()
                ),
                () -> assertNull(execution.getEndTime()),
                () -> assertTrue(execution.getLogs().isEmpty())
        );
    }
}
