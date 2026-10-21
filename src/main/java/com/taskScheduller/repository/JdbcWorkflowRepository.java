
package com.taskScheduller.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskScheduller.domain.Step;
import com.taskScheduller.domain.Trigger;
import com.taskScheduller.domain.Workflow;
import com.taskScheduller.domain.enums.ActionType;
import com.taskScheduller.domain.enums.TriggerType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcWorkflowRepository implements WorkflowRepository {

    private static final TypeReference<Map<String, String>> CONFIG_TYPE =
            new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcWorkflowRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Workflow save(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow must not be null");

        jdbcTemplate.update("""
                INSERT INTO workflows (
                    id,
                    name,
                    description,
                    enabled,
                    trigger_type,
                    interval_seconds,
                    next_run_time,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    enabled = EXCLUDED.enabled,
                    trigger_type = EXCLUDED.trigger_type,
                    interval_seconds = EXCLUDED.interval_seconds,
                    next_run_time = EXCLUDED.next_run_time
                """,
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.isEnabled(),
                workflow.getTrigger().getType().name(),
                workflow.getTrigger().getIntervalSeconds(),
                toTimestamp(workflow.getTrigger().getNextRunTime()),
                Timestamp.from(workflow.getCreatedAt())
        );

        jdbcTemplate.update(
                "DELETE FROM workflow_steps WHERE workflow_id = ?",
                workflow.getId()
        );

        for (Step step : workflow.getSteps()) {
            jdbcTemplate.update("""
                    INSERT INTO workflow_steps (
                        id,
                        workflow_id,
                        name,
                        step_order,
                        action_type,
                        config,
                        max_retries,
                        retry_backoff_millis
                    )
                    VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                    """,
                    step.getId(),
                    workflow.getId(),
                    step.getName(),
                    step.getOrder(),
                    step.getActionType().name(),
                    serializeConfig(step.getConfig()),
                    step.getMaxRetries(),
                    step.getRetryBackoffMillis()
            );
        }

        return workflow;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findById(String id) {
        List<WorkflowRow> rows = jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    description,
                    enabled,
                    trigger_type,
                    interval_seconds,
                    next_run_time,
                    created_at
                FROM workflows
                WHERE id = ?
                """,
                (rs, rowNum) -> mapWorkflowRow(rs),
                id
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        WorkflowRow row = rows.getFirst();

        return Optional.of(restoreWorkflow(
                row,
                findStepsByWorkflowId(row.id())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findAll() {
        List<WorkflowRow> workflows = jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    description,
                    enabled,
                    trigger_type,
                    interval_seconds,
                    next_run_time,
                    created_at
                FROM workflows
                ORDER BY created_at, id
                """,
                (rs, rowNum) -> mapWorkflowRow(rs)
        );

        if (workflows.isEmpty()) {
            return List.of();
        }

        Map<String, List<Step>> stepsByWorkflow = new HashMap<>();

        jdbcTemplate.query(
                """
                SELECT
                    workflow_id,
                    id,
                    name,
                    step_order,
                    action_type,
                    config,
                    max_retries,
                    retry_backoff_millis
                FROM workflow_steps
                ORDER BY workflow_id, step_order
                """,
                rs -> {
                    String workflowId = rs.getString("workflow_id");

                    stepsByWorkflow
                            .computeIfAbsent(
                                    workflowId,
                                    key -> new ArrayList<>()
                            )
                            .add(mapStep(rs));
                }
        );

        return workflows.stream()
                .map(row -> restoreWorkflow(
                        row,
                        stepsByWorkflow.getOrDefault(
                                row.id(),
                                List.of()
                        )
                ))
                .toList();
    }

    private List<Step> findStepsByWorkflowId(String workflowId) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    step_order,
                    action_type,
                    config,
                    max_retries,
                    retry_backoff_millis
                FROM workflow_steps
                WHERE workflow_id = ?
                ORDER BY step_order
                """,
                (rs, rowNum) -> mapStep(rs),
                workflowId
        );
    }

    private WorkflowRow mapWorkflowRow(ResultSet rs)
            throws SQLException {

        Timestamp nextRun = rs.getTimestamp("next_run_time");

        return new WorkflowRow(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("enabled"),
                TriggerType.valueOf(rs.getString("trigger_type")),
                rs.getLong("interval_seconds"),
                nextRun == null ? null : nextRun.toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private Step mapStep(ResultSet rs) throws SQLException {
        return Step.restore(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("step_order"),
                ActionType.valueOf(rs.getString("action_type")),
                deserializeConfig(rs.getString("config")),
                rs.getInt("max_retries"),
                rs.getLong("retry_backoff_millis")
        );
    }

    private Workflow restoreWorkflow(
            WorkflowRow row,
            List<Step> steps
    ) {
        Trigger trigger = Trigger.restore(
                row.triggerType(),
                row.intervalSeconds(),
                row.nextRunTime()
        );

        return Workflow.restore(
                row.id(),
                row.name(),
                row.description(),
                trigger,
                row.enabled(),
                row.createdAt(),
                steps
        );
    }

    private String serializeConfig(Map<String, String> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize step configuration",
                    e
            );
        }
    }

    private Map<String, String> deserializeConfig(String json) {
        try {
            return objectMapper.readValue(json, CONFIG_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize step configuration",
                    e
            );
        }
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private record WorkflowRow(
            String id,
            String name,
            String description,
            boolean enabled,
            TriggerType triggerType,
            long intervalSeconds,
            Instant nextRunTime,
            Instant createdAt
    ) {}
}
