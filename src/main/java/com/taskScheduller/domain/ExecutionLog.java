
package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.StepStatus;

import java.time.Instant;
import java.util.Objects;

public class ExecutionLog {

    private final String stepId;
    private final String stepName;
    private final StepStatus status;
    private final String message;
    private final int attemptNumber;
    private final Instant timestamp;

    public ExecutionLog(
            String stepId,
            String stepName,
            StepStatus status,
            String message,
            int attemptNumber
    ) {
        this(
                stepId,
                stepName,
                status,
                message,
                attemptNumber,
                Instant.now()
        );
    }

    private ExecutionLog(
            String stepId,
            String stepName,
            StepStatus status,
            String message,
            int attemptNumber,
            Instant timestamp
    ) {
        this.stepId = Objects.requireNonNull(stepId, "stepId must not be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.message = message;
        this.attemptNumber = attemptNumber;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static ExecutionLog restore(
            String stepId,
            String stepName,
            StepStatus status,
            String message,
            int attemptNumber,
            Instant timestamp
    ) {
        return new ExecutionLog(
                stepId,
                stepName,
                status,
                message,
                attemptNumber,
                timestamp
        );
    }

    public String getStepId() {
        return stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public StepStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[%s] step='%s' status=%s attempt=%d msg='%s'"
                .formatted(timestamp, stepName, status, attemptNumber, message);
    }
}
