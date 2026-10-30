
package com.taskScheduller.engine;

import com.taskScheduller.domain.*;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.StepStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowExecutor {

    private final Map<ActionType, ActionExecutor> executors;

    private enum StepExecutionResult {
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    public WorkflowExecutor(List<ActionExecutor> actionExecutors) {
        this.executors = new HashMap<>();

        for (ActionExecutor executor : actionExecutors) {
            executors.put(executor.supports(), executor);
        }
    }

    private StepExecutionResult runStepWithRetries(
            Step step,
            Execution execution
    ) {
        if (Thread.currentThread().isInterrupted()) {
            return StepExecutionResult.CANCELLED;
        }

        ActionExecutor executor = executors.get(step.getActionType());

        if (executor == null) {
            execution.addLog(new ExecutionLog(
                    step.getId(),
                    step.getName(),
                    StepStatus.FAILED,
                    "No executor registered for action type "
                            + step.getActionType(),
                    1
            ));

            return StepExecutionResult.FAILED;
        }

        int attempt = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return StepExecutionResult.CANCELLED;
            }

            attempt++;

            try {
                executor.execute(step.getConfig());

                if (Thread.currentThread().isInterrupted()) {
                    return StepExecutionResult.CANCELLED;
                }

                execution.addLog(new ExecutionLog(
                        step.getId(),
                        step.getName(),
                        StepStatus.SUCCEEDED,
                        "Step completed successfully",
                        attempt
                ));

                return StepExecutionResult.SUCCEEDED;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return StepExecutionResult.CANCELLED;

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    return StepExecutionResult.CANCELLED;
                }

                boolean hasRetriesLeft = attempt <= step.getMaxRetries();

                StepStatus status = hasRetriesLeft
                        ? StepStatus.RETRYING
                        : StepStatus.FAILED;

                execution.addLog(new ExecutionLog(
                        step.getId(),
                        step.getName(),
                        status,
                        e.getMessage(),
                        attempt
                ));

                if (!hasRetriesLeft) {
                    return StepExecutionResult.FAILED;
                }

                ThreadUtils.sleepQuietly(step.getRetryBackoffMillis());

                if (Thread.currentThread().isInterrupted()) {
                    return StepExecutionResult.CANCELLED;
                }
            }
        }
    }

    public Execution execute(Workflow workflow) {
        return execute(workflow, new Execution(workflow.getId()));
    }

    public Execution execute(Workflow workflow, Execution execution) {
        execution.markRunning();

        boolean failed = false;
        boolean cancelled = false;

        for (Step step : workflow.getSteps()) {
            if (Thread.currentThread().isInterrupted()) {
                cancelled = true;
                break;
            }

            if (failed) {
                execution.addLog(new ExecutionLog(
                        step.getId(),
                        step.getName(),
                        StepStatus.SKIPPED,
                        "Skipped because a previous step failed",
                        1
                ));

                continue;
            }

            StepExecutionResult result = runStepWithRetries(
                    step,
                    execution
            );

            if (result == StepExecutionResult.FAILED) {
                failed = true;
            } else if (result == StepExecutionResult.CANCELLED) {
                cancelled = true;
                break;
            }
        }

        if (cancelled || Thread.currentThread().isInterrupted()) {
            execution.markCancelled();
        } else if (failed) {
            execution.markFailed();
        } else {
            execution.markSucceeded();
        }

        return execution;
    }
}
