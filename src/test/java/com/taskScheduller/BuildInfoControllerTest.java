
package com.taskScheduller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildInfoControllerTest {

    @Test
    void shouldReturnApplicationAndCommit() {
        BuildInfoController controller = new BuildInfoController("abc123");

        Map<String, String> response = controller.getBuildInfo();

        assertEquals("aris", response.get("application"));
        assertEquals("abc123", response.get("commit"));
    }
}
