package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.StepStatus;

import java.time.Instant;

public class ExecutionLog {

    private final String stepId;
    private final String stepName;
    private final StepStatus status;
    private final String message;
    private final int attemptNumber;
    private final Instant timestamp;

    public ExecutionLog(String stepId, String stepName, StepStatus status, String message, int attemptNumber) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.status = status;
        this.message = message;
        this.attemptNumber = attemptNumber;
        this.timestamp = Instant.now();
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