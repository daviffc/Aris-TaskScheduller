package com.taskScheduller.engine;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.ExecutionLog;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;
import org.junit.jupiter.api.Test;

import com.taskScheduller.domain.enums.ExecutionStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutorTest {

    private Step step(String name, int order, ActionType type, int maxRetries) {
        return new Step(name, order, type, Map.of(), maxRetries, 0);
    }

    private Workflow workflowWith(Step... steps) {
        Workflow workflow = new Workflow("test", "", Trigger.manual());
        for (Step s : steps) {
            workflow.addStep(s);
        }
        return workflow;
    }

    @Test
    void singleStepThatSucceeds_marksExecutionSucceeded() {
        FakeActionExecutor fake = new FakeActionExecutor(ActionType.LOG_MESSAGE, 0);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(fake));

        Execution execution = executor.execute(workflowWith(step("only", 1, ActionType.LOG_MESSAGE, 0)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(execution.getEndTime()).isNotNull();
        assertThat(execution.getLogs()).hasSize(1);
        assertThat(execution.getLogs().get(0).getStatus()).isEqualTo(StepStatus.SUCCEEDED);
        assertThat(fake.getCalls()).isEqualTo(1);
    }

    @Test
    void execution_isRunning_whileAStepIsExecuting() {
        Workflow workflow = workflowWith(step("only", 1, ActionType.LOG_MESSAGE, 0));
        Execution execution = new Execution(workflow.getId());
        AtomicReference<ExecutionStatus> statusSeenByStep = new AtomicReference<>();

        ActionExecutor spy = new ActionExecutor() {
            @Override
            public ActionType supports() {
                return ActionType.LOG_MESSAGE;
            }

            @Override
            public void execute(Map<String, String> config) {
                statusSeenByStep.set(execution.getStatus());
            }
        };

        new WorkflowExecutor(List.of(spy)).execute(workflow, execution);

        assertThat(statusSeenByStep.get()).isEqualTo(ExecutionStatus.RUNNING);
    }

    @Test
    void stepFailingOnce_isRetriedAndThenSucceeds() {
        FakeActionExecutor fake = new FakeActionExecutor(ActionType.LOG_MESSAGE, 1);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(fake));

        Execution execution = executor.execute(workflowWith(step("flaky", 1, ActionType.LOG_MESSAGE, 2)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(execution.getLogs()).extracting(ExecutionLog::getStatus)
                .containsExactly(StepStatus.RETRYING, StepStatus.SUCCEEDED);
        assertThat(execution.getLogs()).extracting(ExecutionLog::getAttemptNumber)
                .containsExactly(1, 2);
        assertThat(fake.getCalls()).isEqualTo(2);
    }

    @Test
    void retriesExhausted_marksExecutionFailed() {
        FakeActionExecutor fake = new FakeActionExecutor(ActionType.LOG_MESSAGE, 99);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(fake));

        Execution execution = executor.execute(workflowWith(step("broken", 1, ActionType.LOG_MESSAGE, 2)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getLogs()).extracting(ExecutionLog::getStatus)
                .containsExactly(StepStatus.RETRYING, StepStatus.RETRYING, StepStatus.FAILED);
        assertThat(fake.getCalls()).isEqualTo(3); // original attempt + 2 retries
    }

    @Test
    void zeroRetries_failsOnFirstError() {
        FakeActionExecutor fake = new FakeActionExecutor(ActionType.LOG_MESSAGE, 99);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(fake));

        Execution execution = executor.execute(workflowWith(step("once", 1, ActionType.LOG_MESSAGE, 0)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getLogs()).extracting(ExecutionLog::getStatus)
                .containsExactly(StepStatus.FAILED);
        assertThat(fake.getCalls()).isEqualTo(1);
    }

    @Test
    void afterAFailure_remainingStepsAreSkippedAndNeverRun() {
        FakeActionExecutor failing = new FakeActionExecutor(ActionType.LOG_MESSAGE, 99);
        FakeActionExecutor neverCalled = new FakeActionExecutor(ActionType.HTTP_REQUEST, 0);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(failing, neverCalled));

        Execution execution = executor.execute(workflowWith(
                step("a", 1, ActionType.LOG_MESSAGE, 0),
                step("b", 2, ActionType.HTTP_REQUEST, 0)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getLogs()).extracting(ExecutionLog::getStatus)
                .containsExactly(StepStatus.FAILED, StepStatus.SKIPPED);
        assertThat(neverCalled.getCalls()).isZero();
    }

    @Test
    void missingExecutor_failsTheStepWithAClearMessage() {
        WorkflowExecutor executor = new WorkflowExecutor(List.of());

        Execution execution = executor.execute(workflowWith(step("orphan", 1, ActionType.LOG_MESSAGE, 0)));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getLogs()).hasSize(1);
        assertThat(execution.getLogs().get(0).getStatus()).isEqualTo(StepStatus.FAILED);
        assertThat(execution.getLogs().get(0).getMessage()).contains("No executor registered");
    }

    @Test
    void stepsRunByOrderField_notByInsertionOrder() {
        List<String> seen = new ArrayList<>();
        ActionExecutor recorder = new ActionExecutor() {
            @Override
            public ActionType supports() {
                return ActionType.LOG_MESSAGE;
            }

            @Override
            public void execute(Map<String, String> config) {
                seen.add(config.get("id"));
            }
        };
        Workflow workflow = new Workflow("test", "", Trigger.manual());
        workflow.addStep(new Step("second", 2, ActionType.LOG_MESSAGE, Map.of("id", "second"), 0, 0));
        workflow.addStep(new Step("first", 1, ActionType.LOG_MESSAGE, Map.of("id", "first"), 0, 0));

        new WorkflowExecutor(List.of(recorder)).execute(workflow);

        assertThat(seen).containsExactly("first", "second");
    }

    @Test
    void workflowWithoutSteps_succeedsWithNoLogs() {
        Execution execution = new WorkflowExecutor(List.of()).execute(workflowWith());

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(execution.getLogs()).isEmpty();
    }

    @Test
    void execute_createsAnExecutionLinkedToTheWorkflow() {
        Workflow workflow = workflowWith();

        Execution execution = new WorkflowExecutor(List.of()).execute(workflow);

        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
    }


    @Test
    void interruptedAction_cancelsExecutionWithoutRetrying() {
        AtomicInteger calls = new AtomicInteger();

        ActionExecutor interrupting = new ActionExecutor() {
            @Override
            public ActionType supports() {
                return ActionType.LOG_MESSAGE;
            }

            @Override
            public void execute(Map<String, String> config)
                    throws Exception {
                calls.incrementAndGet();
                throw new InterruptedException("simulated interruption");
            }
        };

        try {
            Thread.interrupted();

            Execution execution = new WorkflowExecutor(List.of(interrupting))
                    .execute(workflowWith(
                            step("interruptible", 1, ActionType.LOG_MESSAGE, 3)
                    ));

            assertThat(execution.getStatus())
                    .isEqualTo(ExecutionStatus.CANCELLED);
            assertThat(execution.getEndTime()).isNotNull();
            assertThat(calls.get()).isEqualTo(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();

        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptionAfterFailedAttempt_cancelsWithoutRetrying()
            throws Exception {
        CountDownLatch firstAttemptStarted = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Execution> result = new AtomicReference<>();

        ActionExecutor failing = new ActionExecutor() {
            @Override
            public ActionType supports() {
                return ActionType.LOG_MESSAGE;
            }

            @Override
            public void execute(Map<String, String> config) {
                calls.incrementAndGet();
                firstAttemptStarted.countDown();
                throw new IllegalStateException("temporary failure");
            }
        };

        Step retryStep = new Step(
                "interruptible",
                1,
                ActionType.LOG_MESSAGE,
                Map.of(),
                3,
                10_000
        );

        Workflow workflow = workflowWith(retryStep);

        Thread worker = new Thread(
                () -> result.set(
                        new WorkflowExecutor(List.of(failing)).execute(workflow)
                ),
                "workflow-interruption-test"
        );

        worker.start();

        try {
            assertThat(firstAttemptStarted.await(2, TimeUnit.SECONDS)).isTrue();

            Thread.sleep(50);
            worker.interrupt();
            worker.join(2_000);

            assertThat(worker.isAlive()).isFalse();
            assertThat(result.get()).isNotNull();
            assertThat(result.get().getStatus())
                    .isEqualTo(ExecutionStatus.CANCELLED);
            assertThat(calls.get()).isEqualTo(1);

        } finally {
            worker.interrupt();
            worker.join(2_000);
        }
    }

}