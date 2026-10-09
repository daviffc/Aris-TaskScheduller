package com.taskScheduller.repository;

import com.taskScheduller.domain.Execution;

import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {
    Execution save(Execution execution);
    Optional<Execution> findById(String id);
    List<Execution> findByWorkflowId(String workflowId);
}