
package com.taskScheduller.service;

import com.taskScheduller.api.CreateWorkflowRequest;
import com.taskScheduller.api.StepRequest;
import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import com.taskScheduller.engine.WorkflowExecutor;
import com.taskScheduller.engine.WorkflowScheduller;
import com.taskScheduller.repository.ExecutionRepository;
import com.taskScheduller.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final WorkflowExecutor executor;
    private final ExecutorService executionPool;
    private final WorkflowScheduller scheduler;

    public WorkflowService(
            WorkflowRepository workflowRepository,
            WorkflowScheduller scheduler,
            ExecutionRepository executionRepository,
            WorkflowExecutor executor,
            ExecutorService executionPool
    ) {
        this.workflowRepository = workflowRepository;
        this.scheduler = scheduler;
        this.executionRepository = executionRepository;
        this.executor = executor;
        this.executionPool = executionPool;
    }

    public Workflow create(CreateWorkflowRequest request) {
        Trigger trigger = switch (request.triggerType()) {
            case CRON -> {
                if (request.intervalSeconds() == null) {
                    throw new IllegalArgumentException(
                            "intervalSeconds is required for CRON triggers"
                    );
                }

                yield Trigger.cron(request.intervalSeconds());
            }

            case MANUAL -> Trigger.manual();

            case WEBHOOK -> throw new IllegalArgumentException(
                    "WEBHOOK triggers are not supported yet"
            );
        };

        String description = request.description() == null
                ? ""
                : request.description();

        Workflow workflow = new Workflow(
                request.name(),
                description,
                trigger
        );

        int order = 1;

        for (StepRequest step : request.steps()) {
            workflow.addStep(new Step(
                    step.name(),
                    order++,
                    step.actionType(),
                    step.config() == null
                            ? Map.of()
                            : step.config(),
                    step.maxRetries() == null
                            ? 0
                            : step.maxRetries(),
                    step.retryBackoffMillis() == null
                            ? 1000
                            : step.retryBackoffMillis()
            ));
        }

        workflowRepository.save(workflow);

        if (trigger.getType() == TriggerType.CRON) {
            scheduler.schedule(workflow);
        }

        return workflow;
    }

    public Optional<Execution> trigger(String workflowId) {
        return workflowRepository.findById(workflowId).map(workflow -> {
            Execution execution = new Execution(workflow.getId());

            executionRepository.save(execution);

            executionPool.submit(() -> {
                System.out.println(
                        "Running execution " + execution.getId()
                                + " on thread "
                                + Thread.currentThread().getName()
                );

                try {
                    execution.markRunning();
                    executionRepository.save(execution);

                    executor.execute(workflow, execution);

                } catch (Exception e) {
                    execution.markFailed();

                    System.err.println(
                            "Execution " + execution.getId()
                                    + " crashed: " + e
                    );

                } finally {
                    try {
                        executionRepository.save(execution);
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to persist execution "
                                        + execution.getId()
                                        + ": " + e
                        );
                    }
                }
            });

            return execution;
        });
    }
}
