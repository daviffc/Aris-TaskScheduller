
package com.taskScheduller.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Workflow {

    private final String id;
    private String name;
    private String description;
    private Trigger trigger;
    private final List<Step> steps;
    private boolean enabled;
    private final Instant createdAt;

    public Workflow(String name, String description, Trigger trigger) {
        this(
                UUID.randomUUID().toString(),
                name,
                description,
                trigger,
                true,
                Instant.now()
        );
    }

    private Workflow(
            String id,
            String name,
            String description,
            Trigger trigger,
            boolean enabled,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.trigger = Objects.requireNonNull(trigger, "trigger must not be null");
        this.steps = new ArrayList<>();
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Workflow restore(
            String id,
            String name,
            String description,
            Trigger trigger,
            boolean enabled,
            Instant createdAt,
            List<Step> steps
    ) {
        Workflow workflow = new Workflow(
                id,
                name,
                description,
                trigger,
                enabled,
                createdAt
        );

        Objects.requireNonNull(steps, "steps must not be null")
                .forEach(workflow::addStep);

        return workflow;
    }

    public void addStep(Step step) {
        steps.add(Objects.requireNonNull(step, "step must not be null"));
        steps.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger, "trigger must not be null");
    }

    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workflow workflow)) return false;
        return id.equals(workflow.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Workflow{id='%s', name='%s', steps=%d, enabled=%s}"
                .formatted(id, name, steps.size(), enabled);
    }
}
