package com.taskScheduller.actions;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.enums.ActionType;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class LogMessageExecutor implements ActionExecutor {

    @Override
    public ActionType supports() {
        return ActionType.LOG_MESSAGE;
    }

    @Override
    public void execute(Map<String, String> config) {
        String message = config.get("message");
        if (message == null) {
            throw new IllegalArgumentException("LOG_MESSAGE step requires a 'message' config key");
        }
        System.out.println("[LOG_MESSAGE] " + message);
    }
}