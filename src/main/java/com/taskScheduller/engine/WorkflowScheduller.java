package com.taskScheduller.engine;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.taskScheduller.repository.ExecutionRepository;

import java.util.PriorityQueue;

@Component
public class WorkflowScheduller {
    private final PriorityQueue<Workflow> queue;
    private volatile boolean running = false;
    private Thread worker;
    private final WorkflowExecutor executor;
    private final ExecutionRepository executionRepository;


    public WorkflowScheduller(WorkflowExecutor executor, ExecutionRepository executionRepository) {
        this.queue = new PriorityQueue<>(
                (a,b) -> a.getTrigger().getNextRunTime().compareTo(b.getTrigger().getNextRunTime())
        );
        this.executor = executor;
        this.executionRepository = executionRepository;
    }
    public void schedule(Workflow workflow) {
        if (workflow.getTrigger().getType() != TriggerType.CRON) {
            throw new IllegalArgumentException("cron trigger type must be CRON");
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

    public void start() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(this::runLoop);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        System.out.println("Scheduler stopping...");
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void runLoop() {
        while (running) {
            if (isEmpty()) {
                ThreadUtils.sleepQuietly(500);
                continue;
            }

            Workflow next = peekNext();
            long millisUntilDue = Duration.between(Instant.now(), next.getTrigger().getNextRunTime()).toMillis();

            if (millisUntilDue <= 0) {
                Workflow due = pollNext();
                Execution execution = executor.execute(due);
                executionRepository.save(execution);
                System.out.println("Execution " + execution.getId() + " finished with status: " + execution.getStatus()
                +"(stored for this workflow: "+ executionRepository.findByWorkflowId(due.getId()).size()+")");
                due.getTrigger().advanceNextRunTime();
                queue.add(due);
            } else {
                ThreadUtils.sleepQuietly(Math.min(millisUntilDue, 500));
            }
        }
    }
}