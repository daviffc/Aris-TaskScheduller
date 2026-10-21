
package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ExecutionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        this(
                UUID.randomUUID().toString(),
                workflowId,
                ExecutionStatus.PENDING,
                Instant.now(),
                null,
                List.of()
        );
    }

    private Execution(
            String id,
            String workflowId,
            ExecutionStatus status,
            Instant startTime,
            Instant endTime,
            List<ExecutionLog> logs
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = endTime;
        this.logs = new CopyOnWriteArrayList<>(
                Objects.requireNonNull(logs, "logs must not be null")
        );
    }

    public static Execution restore(
            String id,
            String workflowId,
            ExecutionStatus status,
            Instant startTime,
            Instant endTime,
            List<ExecutionLog> logs
    ) {
        return new Execution(
                id,
                workflowId,
                status,
                startTime,
                endTime,
                logs
        );
    }

    public void addLog(ExecutionLog log) {
        logs.add(Objects.requireNonNull(log, "log must not be null"));
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
