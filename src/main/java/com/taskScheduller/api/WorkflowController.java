package com.taskScheduller.api;

import com.taskScheduller.repository.WorkflowRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    public WorkflowController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @GetMapping
    public List<WorkflowResponse> list() {
        return workflowRepository.findAll().stream()
                .map(WorkflowResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> get(@PathVariable String id) {
        return workflowRepository.findById(id)
                .map(WorkflowResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}