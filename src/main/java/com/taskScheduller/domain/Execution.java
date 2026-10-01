package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ExecutionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Execution {
    private final String id;
    private final String workflowId;
    private volatile ExecutionStatus status;
    private final Instant startTime;
    private volatile Instant endTime;
    private final List<ExecutionLog> logs;

    public Execution(String workflowId) {
        this.id = UUID.randomUUID().toString();
        this.workflowId = workflowId;
        this.status = ExecutionStatus.PENDING;
        this.startTime = Instant.now();
        this.logs = new CopyOnWriteArrayList<>();
    }

    public void addLog(ExecutionLog log) {
        logs.add(log);
    }

    public void markRunning() {
        this.status = ExecutionStatus.RUNNING;
    }

    public void markSucceeded() {
        this.status = ExecutionStatus.SUCCEEDED;
        this.endTime = Instant.now();
    }

    public void markFailed() {
        this.status = ExecutionStatus.FAILED;
        this.endTime = Instant.now();
    }

    public void markCancelled() {
        this.status = ExecutionStatus.CANCELLED;
        this.endTime = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public List<ExecutionLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    @Override
    public String toString() {
        return "Execution{id='%s', workflowId='%s', status=%s, logs=%d}"
                .formatted(id, workflowId, status, logs.size());
    }
}
