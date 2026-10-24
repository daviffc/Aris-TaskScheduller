
package com.taskScheduller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BuildInfoController {

    private final String commit;

    public BuildInfoController(@Value("${ARIS_BUILD_SHA:local}") String commit) {
        this.commit = commit;
    }

    @GetMapping("/build-info")
    public Map<String, String> getBuildInfo() {
        return Map.of(
                "application", "aris",
                "commit", commit
        );
    }
}
