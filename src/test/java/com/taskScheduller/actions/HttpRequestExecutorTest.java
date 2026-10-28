
package com.taskScheduller.actions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRequestExecutorTest {

    @Test
    void shouldRejectRequestsWhenDisabled() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(false, "example.com");

        assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        Map.of("url", "https://example.com")
                )
        );
    }

    @Test
    void shouldRejectHttpUrls() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of("url", "http://example.com")
                )
        );
    }

    @Test
    void shouldRejectUnknownHosts() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of("url", "https://another-domain.com")
                )
        );
    }

    @Test
    void shouldRejectUnapprovedMethods() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of(
                                "url", "https://example.com",
                                "method", "DELETE"
                        )
                )
        );
    }

    @Test
    void shouldRejectUserInfoInUrl() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of(
                                "url",
                                "https://user:password@example.com"
                        )
                )
        );
    }
}
