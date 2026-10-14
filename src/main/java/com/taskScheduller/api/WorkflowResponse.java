package com.taskScheduller.api;

import com.taskScheduller.domain.Workflow;

import java.time.Instant;

public record WorkflowResponse(
        String id,
        String name,
        String description,
        boolean enabled,
        int stepCount,
        Instant nextRunTime
) {
    public static WorkflowResponse from(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.isEnabled(),
                workflow.getSteps().size(),
                workflow.getTrigger().getNextRunTime());
    }
}