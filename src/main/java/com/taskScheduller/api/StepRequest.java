package com.taskScheduller.api;

import com.taskScheduller.domain.enums.ActionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record StepRequest(
        @NotBlank String name,
        @NotNull ActionType actionType,
        Map<String, String> config,
        @Min(0) Integer maxRetries,
        @Min(0) Long retryBackoffMillis
) {}