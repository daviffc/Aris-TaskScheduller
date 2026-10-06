package com.taskScheduller.engine;

import com.taskScheduller.domain.*;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.StepStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowExecutor {

    private final Map<ActionType, ActionExecutor> executors;

    public WorkflowExecutor(List<ActionExecutor> actionExecutors) {
        this.executors = new HashMap<>();
        for (ActionExecutor executor : actionExecutors) {
            executors.put(executor.supports(), executor);
        }
    }

    private boolean runStepWithRetries(Step step, Execution execution) {
        ActionExecutor executor = executors.get(step.getActionType());
        if (executor == null) {
            execution.addLog(new ExecutionLog(
                    step.getId(), step.getName(), StepStatus.FAILED,
                    "No executor registered for action type " + step.getActionType(), 1));
            return false;
        }

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                executor.execute(step.getConfig());
                execution.addLog(new ExecutionLog(
                        step.getId(), step.getName(), StepStatus.SUCCEEDED,
                        "Step completed successfully", attempt));
                return true;
            } catch (Exception e) {
                boolean hasRetriesLeft = attempt <= step.getMaxRetries();
                StepStatus status = hasRetriesLeft ? StepStatus.RETRYING : StepStatus.FAILED;
                execution.addLog(new ExecutionLog(
                        step.getId(), step.getName(), status, e.getMessage(), attempt));

                if (!hasRetriesLeft) {
                    return false;
                }
                ThreadUtils.sleepQuietly(step.getRetryBackoffMillis());
            }
        }
    }

    public Execution execute(Workflow workflow) {
        Execution execution = new Execution(workflow.getId());
        execution.markRunning();

        boolean failed = false;

        for (Step step : workflow.getSteps()) {
            if (failed) {
                execution.addLog(new ExecutionLog(
                        step.getId(), step.getName(), StepStatus.SKIPPED,
                        "Skipped because a previous step failed", 1));
                continue;
            }

            boolean success = runStepWithRetries(step, execution);
            if (!success) {
                failed = true;
            }
        }

        if (failed) {
            execution.markFailed();
        } else {
            execution.markSucceeded();
        }

        return execution;
    }
}