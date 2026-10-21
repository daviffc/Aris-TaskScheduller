
package com.taskScheduller.bootstrap;

import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import com.taskScheduller.engine.WorkflowScheduller;
import com.taskScheduller.repository.WorkflowRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStartupRunner implements CommandLineRunner {

    private final WorkflowScheduller scheduler;
    private final WorkflowRepository workflowRepository;

    public WorkflowStartupRunner(
            WorkflowScheduller scheduler,
            WorkflowRepository workflowRepository
    ) {
        this.scheduler = scheduler;
        this.workflowRepository = workflowRepository;
    }

    @Override
    public void run(String... args) {
        int restored = 0;

        for (Workflow workflow : workflowRepository.findAll()) {
            if (workflow.isEnabled()
                    && workflow.getTrigger().getType() == TriggerType.CRON) {
                scheduler.schedule(workflow);
                restored++;
            }
        }

        scheduler.start();

        System.out.println("Scheduler started with " + restored + " restored workflows");
    }
}
