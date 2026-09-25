package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ActionType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Step {
    private final String id;
    private final String name;
    private final int order;
    private final ActionType actionType;
    private final Map<String, String> config;
    private final int maxRetries;
    private final long retryBackoffMillis;

    public Step(String name, int order, ActionType actionType, Map<String, String> config,
                int maxRetries, long retryBackoffMillis) {
        this.id = UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.order = order;
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.config = Map.copyOf(config); // defensive copy, immutable
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        this.maxRetries = maxRetries;
        this.retryBackoffMillis = retryBackoffMillis;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Step step)) return false;
        return id.equals(step.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Step{id='%s', name='%s', order=%d, actionType=%s}"
                .formatted(id, name, order, actionType);
    }
}
