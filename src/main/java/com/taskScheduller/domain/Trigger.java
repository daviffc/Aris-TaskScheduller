package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.TriggerType;

import java.time.Instant;
import java.util.Objects;

public class Trigger {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return getIntervalSeconds() == trigger.getIntervalSeconds() && getType() == trigger.getType() && Objects.equals(getNextRunTime(), trigger.getNextRunTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getIntervalSeconds(), getNextRunTime());
    }

    private final TriggerType type;
    private final long intervalSeconds;
    private volatile Instant nextRunTime;

    private Trigger(TriggerType type, long intervalSeconds, Instant nextRunTime) {
        this.type = type;
        this.intervalSeconds = intervalSeconds;
        this.nextRunTime = nextRunTime;
    }

    public static Trigger cron(long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }
        return new Trigger(TriggerType.CRON, intervalSeconds, Instant.now().plusSeconds(intervalSeconds));
    }

    public static Trigger manual() {
        return new Trigger(TriggerType.MANUAL, 0, null);
    }

    public static Trigger webhook() {
        return new Trigger(TriggerType.WEBHOOK, 0, null);
    }

    public void advanceNextRunTime() {
        if (type != TriggerType.CRON) {
            throw new IllegalStateException("Only CRON triggers have a nextRunTime to advance");
        }
        this.nextRunTime = Instant.now().plusSeconds(intervalSeconds);
    }

    public TriggerType getType() {
        return type;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public Instant getNextRunTime() {
        return nextRunTime;
    }

}