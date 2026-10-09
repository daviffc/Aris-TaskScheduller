package com.taskScheduller.repository;

import com.taskScheduller.domain.Workflow;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public Workflow save(Workflow workflow) {
        store.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Workflow> findAll() {
        return List.copyOf(store.values());
    }
}