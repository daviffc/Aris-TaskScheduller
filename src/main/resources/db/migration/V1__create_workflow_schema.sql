
CREATE TABLE workflows (
                           id VARCHAR(36) PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           description TEXT,
                           enabled BOOLEAN NOT NULL,
                           trigger_type VARCHAR(30) NOT NULL,
                           interval_seconds BIGINT NOT NULL,
                           next_run_time TIMESTAMPTZ,
                           created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE workflow_steps (
                                id VARCHAR(36) PRIMARY KEY,
                                workflow_id VARCHAR(36) NOT NULL,
                                name VARCHAR(255) NOT NULL,
                                step_order INTEGER NOT NULL,
                                action_type VARCHAR(40) NOT NULL,
                                config JSONB NOT NULL,
                                max_retries INTEGER NOT NULL,
                                retry_backoff_millis BIGINT NOT NULL,
                                CONSTRAINT fk_step_workflow
                                    FOREIGN KEY (workflow_id)
                                        REFERENCES workflows(id)
                                        ON DELETE CASCADE,
                                CONSTRAINT uq_step_order
                                    UNIQUE (workflow_id, step_order)
);

CREATE TABLE executions (
                            id VARCHAR(36) PRIMARY KEY,
                            workflow_id VARCHAR(36) NOT NULL,
                            status VARCHAR(30) NOT NULL,
                            start_time TIMESTAMPTZ NOT NULL,
                            end_time TIMESTAMPTZ,
                            CONSTRAINT fk_execution_workflow
                                FOREIGN KEY (workflow_id)
                                    REFERENCES workflows(id)
);

CREATE TABLE execution_logs (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                execution_id VARCHAR(36) NOT NULL,
                                step_id VARCHAR(36) NOT NULL,
                                step_name VARCHAR(255) NOT NULL,
                                status VARCHAR(30) NOT NULL,
                                message TEXT,
                                attempt_number INTEGER NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL,
                                CONSTRAINT fk_log_execution
                                    FOREIGN KEY (execution_id)
                                        REFERENCES executions(id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_steps_workflow
    ON workflow_steps(workflow_id);

CREATE INDEX idx_executions_workflow
    ON executions(workflow_id);

CREATE INDEX idx_logs_execution
    ON execution_logs(execution_id);

CREATE INDEX idx_workflows_next_run
    ON workflows(next_run_time)
    WHERE enabled = TRUE;
