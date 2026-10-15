package com.taskScheduller.api;

import com.taskScheduller.repository.ExecutionRepository;
import com.taskScheduller.service.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class ExecutionController {

    private final WorkflowService workflowService;
    private final ExecutionRepository executionRepository;

    public ExecutionController(WorkflowService workflowService, ExecutionRepository executionRepository) {
        this.workflowService = workflowService;
        this.executionRepository = executionRepository;
    }

    @PostMapping("/workflows/{id}/executions")
    public ResponseEntity<ExecutionResponse> trigger(@PathVariable String id) {
        return workflowService.trigger(id)
                .map(execution -> ResponseEntity.accepted()
                        .location(URI.create("/executions/" + execution.getId()))
                        .body(ExecutionResponse.from(execution)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionResponse> get(@PathVariable String id) {
        return executionRepository.findById(id)
                .map(ExecutionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}