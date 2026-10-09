package com.taskScheduller.repository;

import com.taskScheduller.domain.Execution;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<String, Execution> store = new ConcurrentHashMap<>();

    @Override
    public Execution save(Execution execution) {
        store.put(execution.getId(), execution);
        return execution;
    }

    @Override
    public Optional<Execution> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Execution> findByWorkflowId(String workflowId) {
        return store.values().stream()
                .filter(e -> e.getWorkflowId().equals(workflowId))
                .toList();
    }
}