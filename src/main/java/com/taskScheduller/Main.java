package com.taskScheduller;

import com.taskScheduller.actions.LogMessageExecutor;
import com.taskScheduller.domain.*;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.engine.WorkflowExecutor;
import com.taskScheduller.engine.WorkflowScheduller;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        Workflow workflow = new Workflow("Periodic greeter", "Says hello every 3 seconds", Trigger.cron(3));
        workflow.addStep(new Step("Greet", 1, ActionType.LOG_MESSAGE,
                Map.of("message", "Hello from scheduled execution!"), 0, 1000));


        WorkflowExecutor executor = new WorkflowExecutor(List.of(new LogMessageExecutor()));
        WorkflowScheduller scheduler = new WorkflowScheduller(executor);

        scheduler.schedule(workflow);
        scheduler.start();

        Thread.sleep(15000);

        scheduler.stop();
        System.out.println("Scheduler stopped");
    }
}