package com.taskScheduller.domain;

import com.taskScheduller.domain.enums.ActionType;

import java.util.Map;

public interface ActionExecutor {
    ActionType supports();

    void execute(Map<String, String> config) throws Exception;
}
