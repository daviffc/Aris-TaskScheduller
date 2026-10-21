
package com.taskScheduller.repository;

import com.taskScheduller.domain.Execution;
import com.taskScheduller.domain.ExecutionLog;
import com.taskScheduller.domain.enums.ExecutionStatus;
import com.taskScheduller.domain.enums.StepStatus;
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
public class JdbcExecutionRepository implements ExecutionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Execution save(Execution execution) {
        Objects.requireNonNull(execution, "execution must not be null");

        jdbcTemplate.update("""
                INSERT INTO executions (
                    id,
                    workflow_id,
                    status,
                    start_time,
                    end_time
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    end_time = EXCLUDED.end_time
                """,
                execution.getId(),
                execution.getWorkflowId(),
                execution.getStatus().name(),
                Timestamp.from(execution.getStartTime()),
                toTimestamp(execution.getEndTime())
        );

        jdbcTemplate.update(
                "DELETE FROM execution_logs WHERE execution_id = ?",
                execution.getId()
        );

        for (ExecutionLog log : execution.getLogs()) {
            jdbcTemplate.update("""
                    INSERT INTO execution_logs (
                        execution_id,
                        step_id,
                        step_name,
                        status,
                        message,
                        attempt_number,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    execution.getId(),
                    log.getStepId(),
                    log.getStepName(),
                    log.getStatus().name(),
                    log.getMessage(),
                    log.getAttemptNumber(),
                    Timestamp.from(log.getTimestamp())
            );
        }

        return execution;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Execution> findById(String id) {
        List<ExecutionRow> rows = jdbcTemplate.query(
                """
                SELECT
                    id,
                    workflow_id,
                    status,
                    start_time,
                    end_time
                FROM executions
                WHERE id = ?
                """,
                (rs, rowNum) -> mapExecutionRow(rs),
                id
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        ExecutionRow row = rows.getFirst();

        return Optional.of(
                restoreExecution(
                        row,
                        findLogsByExecutionId(id)
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Execution> findByWorkflowId(String workflowId) {
        List<ExecutionRow> rows = jdbcTemplate.query(
                """
                SELECT
                    id,
                    workflow_id,
                    status,
                    start_time,
                    end_time
                FROM executions
                WHERE workflow_id = ?
                ORDER BY start_time DESC, id
                """,
                (rs, rowNum) -> mapExecutionRow(rs),
                workflowId
        );

        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, List<ExecutionLog>> logsByExecution = new HashMap<>();

        jdbcTemplate.query(
                """
                SELECT
                    l.execution_id,
                    l.step_id,
                    l.step_name,
                    l.status,
                    l.message,
                    l.attempt_number,
                    l.created_at
                FROM execution_logs l
                JOIN executions e ON e.id = l.execution_id
                WHERE e.workflow_id = ?
                ORDER BY l.id
                """,
                rs -> {
                    logsByExecution
                            .computeIfAbsent(
                                    rs.getString("execution_id"),
                                    key -> new ArrayList<>()
                            )
                            .add(mapLog(rs));
                },
                workflowId
        );

        return rows.stream()
                .map(row -> restoreExecution(
                        row,
                        logsByExecution.getOrDefault(
                                row.id(),
                                List.of()
                        )
                ))
                .toList();
    }

    private List<ExecutionLog> findLogsByExecutionId(String executionId) {
        return jdbcTemplate.query(
                """
                SELECT
                    step_id,
                    step_name,
                    status,
                    message,
                    attempt_number,
                    created_at
                FROM execution_logs
                WHERE execution_id = ?
                ORDER BY id
                """,
                (rs, rowNum) -> mapLog(rs),
                executionId
        );
    }

    private ExecutionRow mapExecutionRow(ResultSet rs)
            throws SQLException {

        Timestamp endTime = rs.getTimestamp("end_time");

        return new ExecutionRow(
                rs.getString("id"),
                rs.getString("workflow_id"),
                ExecutionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("start_time").toInstant(),
                endTime == null ? null : endTime.toInstant()
        );
    }

    private ExecutionLog mapLog(ResultSet rs)
            throws SQLException {

        return ExecutionLog.restore(
                rs.getString("step_id"),
                rs.getString("step_name"),
                StepStatus.valueOf(rs.getString("status")),
                rs.getString("message"),
                rs.getInt("attempt_number"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private Execution restoreExecution(
            ExecutionRow row,
            List<ExecutionLog> logs
    ) {
        return Execution.restore(
                row.id(),
                row.workflowId(),
                row.status(),
                row.startTime(),
                row.endTime(),
                logs
        );
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private record ExecutionRow(
            String id,
            String workflowId,
            ExecutionStatus status,
            Instant startTime,
            Instant endTime
    ) {}
}
