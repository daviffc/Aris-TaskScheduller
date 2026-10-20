package com.taskScheduller.engine;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutorTest {

    @Test
    void singleStepThatSucceeds_marksExecutionSucceeded() {
        // given
        FakeActionExecutor fake = new FakeActionExecutor(ActionType.LOG_MESSAGE, 0);
        WorkflowExecutor executor = new WorkflowExecutor(List.of(fake));

        Workflow workflow = new Workflow("test", "", Trigger.manual());
        workflow.addStep(new Step("only step", 1, ActionType.LOG_MESSAGE, Map.of(), 0, 0));

        // when
        Execution execution = executor.execute(workflow);

        // then
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        assertThat(execution.getLogs()).hasSize(1);
        assertThat(execution.getLogs().get(0).getStatus()).isEqualTo(StepStatus.SUCCEEDED);
        assertThat(fake.getCalls()).isEqualTo(1);
    }
}