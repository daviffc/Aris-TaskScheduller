package com.taskScheduller.repository;

import com.taskScheduller.domain.Workflow;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {
    Workflow save(Workflow workflow);
    Optional<Workflow> findById(String id);
    List<Workflow> findAll();
}