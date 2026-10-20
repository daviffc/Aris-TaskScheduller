package com.taskScheduller.engine;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.enums.ActionType;

import java.util.Map;

class FakeActionExecutor implements ActionExecutor {

    private final ActionType type;
    private final int failuresBeforeSuccess;
    private int calls = 0;

    FakeActionExecutor(ActionType type, int failuresBeforeSuccess) {
        this.type = type;
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    public ActionType supports() {
        return type;
    }

    @Override
    public void execute(Map<String, String> config) throws Exception {
        calls++;
        if (calls <= failuresBeforeSuccess) {
            throw new RuntimeException("simulated failure #" + calls);
        }
    }

    int getCalls() {
        return calls;
    }
}