package com.taskScheduller;

import com.taskScheduller.actions.LogMessageExecutor;
import com.taskScheduller.actions.HttpRequestExecutor;
import com.taskScheduller.domain.*;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.engine.WorkflowExecutor;
import com.taskScheduller.engine.WorkflowScheduller;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        Workflow workflow = new Workflow("Multi-step workflow", "Logs and makes HTTP request", Trigger.cron(5));
        workflow.addStep(new Step("Greet", 1, ActionType.LOG_MESSAGE,
                Map.of("message", "Starting workflow..."), 0, 1000));
        workflow.addStep(new Step("Check status", 2, ActionType.HTTP_REQUEST,
                Map.of("url", "https://httpbin.org/status/200", "method", "GET"), 2, 1000));

        WorkflowExecutor executor = new WorkflowExecutor(List.of(
                new LogMessageExecutor(),
                new HttpRequestExecutor()
        ));
        WorkflowScheduller scheduler = new WorkflowScheduller(executor);

        scheduler.schedule(workflow);
        scheduler.start();

        Thread.sleep(15000);

        scheduler.stop();
        System.out.println("Scheduler stopped");
    }
}