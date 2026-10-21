
package com.taskScheduller.engine;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import com.taskScheduller.repository.ExecutionRepository;
import com.taskScheduller.repository.WorkflowRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class WorkflowScheduller {

    private final PriorityBlockingQueue<Workflow> queue;
    private final WorkflowExecutor executor;
    private final ExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;

    private volatile boolean running = false;
    private Thread worker;

    @Autowired
    public WorkflowScheduller(
            WorkflowExecutor executor,
            ExecutionRepository executionRepository,
            WorkflowRepository workflowRepository
    ) {
        this.queue = new PriorityBlockingQueue<>(
                11,
                (a, b) -> a.getTrigger().getNextRunTime()
                        .compareTo(b.getTrigger().getNextRunTime())
        );

        this.executor = executor;
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
    }

    public WorkflowScheduller(
            WorkflowExecutor executor,
            ExecutionRepository executionRepository
    ) {
        this(executor, executionRepository, null);
    }

    public void schedule(Workflow workflow) {
        if (workflow.getTrigger().getType() != TriggerType.CRON) {
            throw new IllegalArgumentException("Trigger type must be CRON");
        }

        if (!workflow.isEnabled()) {
            return;
        }

        queue.add(workflow);
    }

    public Workflow peekNext() {
        return queue.peek();
    }

    public Workflow pollNext() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        worker = new Thread(this::runLoop, "aris-workflow-scheduler");
        worker.start();
    }

    @PreDestroy
    public void stop() {
        Thread currentWorker;

        synchronized (this) {
            if (!running) {
                return;
            }

            System.out.println("Scheduler stopping...");

            running = false;
            currentWorker = worker;

            if (currentWorker != null) {
                currentWorker.interrupt();
            }
        }

        if (currentWorker != null
                && currentWorker != Thread.currentThread()) {
            try {
                currentWorker.join(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (currentWorker.isAlive()) {
                System.err.println(
                        "Scheduler worker did not stop within 30 seconds"
                );
            }
        }
    }

    private void runLoop() {
        while (running) {
            Workflow next = peekNext();

            if (next == null) {
                ThreadUtils.sleepQuietly(500);
                continue;
            }

            long millisUntilDue = Duration.between(
                    Instant.now(),
                    next.getTrigger().getNextRunTime()
            ).toMillis();

            if (millisUntilDue > 0) {
                ThreadUtils.sleepQuietly(Math.min(millisUntilDue, 500));
                continue;
            }

            Workflow due = pollNext();

            if (due == null) {
                continue;
            }

            try {
                Execution execution = executor.execute(due);

                if (running) {
                    executionRepository.save(execution);

                    System.out.println(
                            "Execution " + execution.getId()
                                    + " finished with status: "
                                    + execution.getStatus()
                    );
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println(
                            "Scheduled run of workflow "
                                    + due.getId()
                                    + " crashed: "
                                    + e
                    );
                }
            } finally {
                if (running) {
                    try {
                        due.getTrigger().advanceNextRunTime();

                        if (workflowRepository != null) {
                            workflowRepository.save(due);
                        }

                        if (running && due.isEnabled()) {
                            queue.add(due);
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to persist next run for workflow "
                                        + due.getId()
                                        + ": "
                                        + e
                        );
                    }
                }
            }
        }
    }
}
