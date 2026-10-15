package com.taskScheduller.api;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;

import java.time.Instant;
import java.util.List;

public record ExecutionResponse(
        String id,
        String workflowId,
        ExecutionStatus status,
        Instant startTime,
        Instant endTime,
        List<LogResponse> logs
) {
    public record LogResponse(
            String stepId,
            String stepName,
            StepStatus status,
            String message,
            int attemptNumber,
            Instant timestamp
    ) {}

    public static ExecutionResponse from(Execution execution) {
        List<LogResponse> logs = execution.getLogs().stream()
                .map(l -> new LogResponse(
                        l.getStepId(), l.getStepName(), l.getStatus(),
                        l.getMessage(), l.getAttemptNumber(), l.getTimestamp()))
                .toList();
        return new ExecutionResponse(
                execution.getId(), execution.getWorkflowId(), execution.getStatus(),
                execution.getStartTime(), execution.getEndTime(), logs);
    }
}