package com.taskScheduller.bootstrap;

import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.engine.WorkflowScheduller;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DemoWorkflowRunner implements CommandLineRunner {

    private final WorkflowScheduller scheduler;

    public DemoWorkflowRunner(WorkflowScheduller scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run(String... args) {
        Workflow workflow = new Workflow("Multi-step workflow", "Logs and makes HTTP request", Trigger.cron(5));
        workflow.addStep(new Step("Greet", 1, ActionType.LOG_MESSAGE,
                Map.of("message", "Starting workflow..."), 0, 1000));
        workflow.addStep(new Step("Check status", 2, ActionType.HTTP_REQUEST,
                Map.of("url", "https://httpbin.org/status/200", "method", "GET"), 2, 1000));

        scheduler.schedule(workflow);
        scheduler.start();
    }
}