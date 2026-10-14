package com.taskScheduller.api;

import com.taskScheduller.domain.Workflow;
import com.taskScheduller.repository.WorkflowRepository;
import com.taskScheduller.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowRepository workflowRepository, WorkflowService workflowService) {
        this.workflowRepository = workflowRepository;
        this.workflowService = workflowService;
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

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow workflow = workflowService.create(request);
        URI location = URI.create("/workflows/" + workflow.getId());
        return ResponseEntity.created(location).body(WorkflowResponse.from(workflow));
    }
}