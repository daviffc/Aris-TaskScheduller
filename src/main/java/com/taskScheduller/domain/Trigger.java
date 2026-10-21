
package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.TriggerType;

import java.time.Instant;
import java.util.Objects;

public class Trigger {

    private final TriggerType type;
    private final long intervalSeconds;
    private volatile Instant nextRunTime;

    private Trigger(
            TriggerType type,
            long intervalSeconds,
            Instant nextRunTime
    ) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.intervalSeconds = intervalSeconds;
        this.nextRunTime = nextRunTime;
    }

    public static Trigger cron(long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }

        return new Trigger(
                TriggerType.CRON,
                intervalSeconds,
                Instant.now().plusSeconds(intervalSeconds)
        );
    }

    public static Trigger manual() {
        return new Trigger(TriggerType.MANUAL, 0, null);
    }

    public static Trigger webhook() {
        return new Trigger(TriggerType.WEBHOOK, 0, null);
    }

    public static Trigger restore(
            TriggerType type,
            long intervalSeconds,
            Instant nextRunTime
    ) {
        Objects.requireNonNull(type, "type must not be null");

        if (type == TriggerType.CRON) {
            if (intervalSeconds <= 0 || nextRunTime == null) {
                throw new IllegalArgumentException("Invalid CRON trigger");
            }
        } else if (intervalSeconds != 0 || nextRunTime != null) {
            throw new IllegalArgumentException("Invalid non-CRON trigger");
        }

        return new Trigger(type, intervalSeconds, nextRunTime);
    }

    public void advanceNextRunTime() {
        if (type != TriggerType.CRON) {
            throw new IllegalStateException(
                    "Only CRON triggers have a nextRunTime to advance"
            );
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trigger trigger)) return false;

        return intervalSeconds == trigger.intervalSeconds
                && type == trigger.type
                && Objects.equals(nextRunTime, trigger.nextRunTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, intervalSeconds, nextRunTime);
    }
}
