package com.taskScheduller.engine;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.TriggerType;
import java.time.Duration;
import java.time.Instant;

import java.util.PriorityQueue;

public class WorkflowScheduller {
    private final PriorityQueue<Workflow> queue;
    private volatile boolean running = false;
    private Thread worker;
    private WorkflowExecutor executor;

    public WorkflowScheduller(WorkflowExecutor executor) {
        this.queue = new PriorityQueue<>(
                (a,b) -> a.getTrigger().getNextRunTime().compareTo(b.getTrigger().getNextRunTime())
        );
        this.executor = executor;
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

    public void stop() {
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
                System.out.println("Execution " + execution.getId() + " finished with status: " + execution.getStatus());
                due.getTrigger().advanceNextRunTime();
                queue.add(due);
            } else {
                ThreadUtils.sleepQuietly(Math.min(millisUntilDue, 500));
            }
        }
    }
}