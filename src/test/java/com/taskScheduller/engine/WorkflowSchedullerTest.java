package com.taskScheduller.engine;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.repository.ExecutionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowSchedullerTest {

    /** Repository whose first save() blows up, to simulate a crash inside a scheduled run. */
    private static class ThrowingOnceRepository implements ExecutionRepository {
        final AtomicInteger saves = new AtomicInteger();

        @Override
        public Execution save(Execution execution) {
            if (saves.incrementAndGet() == 1) {
                throw new RuntimeException("boom");
            }
            return execution;
        }

        @Override
        public Optional<Execution> findById(String id) {
            return Optional.empty();
        }

        @Override
        public List<Execution> findByWorkflowId(String workflowId) {
            return List.of();
        }
    }

    private WorkflowScheduller newScheduler(ExecutionRepository repository) {
        WorkflowExecutor executor = new WorkflowExecutor(
                List.of(new FakeActionExecutor(ActionType.LOG_MESSAGE, 0)));
        return new WorkflowScheduller(executor, repository);
    }

    private Workflow cronWorkflow(long seconds) {
        Workflow workflow = new Workflow("w", "", Trigger.cron(seconds));
        workflow.addStep(new Step("s", 1, ActionType.LOG_MESSAGE, Map.of(), 0, 0));
        return workflow;
    }

    @Test
    void newScheduler_isEmpty() {
        WorkflowScheduller scheduler = newScheduler(new ThrowingOnceRepository());

        assertThat(scheduler.isEmpty()).isTrue();
        assertThat(scheduler.size()).isZero();
        assertThat(scheduler.peekNext()).isNull();
        assertThat(scheduler.pollNext()).isNull();
    }

    @Test
    void schedule_rejectsNonCronWorkflows() {
        WorkflowScheduller scheduler = newScheduler(new ThrowingOnceRepository());
        Workflow manual = new Workflow("m", "", Trigger.manual());

        assertThatThrownBy(() -> scheduler.schedule(manual))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void soonestWorkflow_comesOutFirst() {
        WorkflowScheduller scheduler = newScheduler(new ThrowingOnceRepository());
        Workflow later = cronWorkflow(60);
        Workflow sooner = cronWorkflow(5);

        scheduler.schedule(later);
        scheduler.schedule(sooner);

        assertThat(scheduler.size()).isEqualTo(2);
        assertThat(scheduler.peekNext()).isSameAs(sooner);
        assertThat(scheduler.pollNext()).isSameAs(sooner);
        assertThat(scheduler.pollNext()).isSameAs(later);
    }

    @Test
    void schedulerKeepsRunning_afterARunCrashes() throws Exception {
        ThrowingOnceRepository repository = new ThrowingOnceRepository();
        WorkflowScheduller scheduler = newScheduler(repository);
        scheduler.schedule(cronWorkflow(1));
        scheduler.start();

        try {
            long deadline = System.currentTimeMillis() + 6000;
            while (repository.saves.get() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            // 1st run crashed on save(); a 2nd run proves the thread survived and the workflow was re-queued
            assertThat(repository.saves.get()).isGreaterThanOrEqualTo(2);
        } finally {
            scheduler.stop();
        }
    }
}