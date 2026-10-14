package com.taskScheduller.service;

import com.taskScheduller.api.CreateWorkflowRequest;
import com.taskScheduller.api.StepRequest;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import com.taskScheduller.engine.WorkflowScheduller;
import com.taskScheduller.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowScheduller scheduler;

    public WorkflowService(WorkflowRepository workflowRepository, WorkflowScheduller scheduler) {
        this.workflowRepository = workflowRepository;
        this.scheduler = scheduler;
    }

    public Workflow create(CreateWorkflowRequest request) {
        Trigger trigger = switch (request.triggerType()) {
            case CRON -> {
                if (request.intervalSeconds() == null) {
                    throw new IllegalArgumentException("intervalSeconds is required for CRON triggers");
                }
                yield Trigger.cron(request.intervalSeconds());
            }
            case MANUAL -> Trigger.manual();
            case WEBHOOK -> throw new IllegalArgumentException("WEBHOOK triggers are not supported yet");
        };

        String description = request.description() == null ? "" : request.description();
        Workflow workflow = new Workflow(request.name(), description, trigger);

        int order = 1;
        for (StepRequest s : request.steps()) {
            workflow.addStep(new Step(
                    s.name(),
                    order++,
                    s.actionType(),
                    s.config() == null ? Map.of() : s.config(),
                    s.maxRetries() == null ? 0 : s.maxRetries(),
                    s.retryBackoffMillis() == null ? 1000 : s.retryBackoffMillis()));
        }

        workflowRepository.save(workflow);
        if (trigger.getType() == TriggerType.CRON) {
            scheduler.schedule(workflow);
        }
        return workflow;
    }
}