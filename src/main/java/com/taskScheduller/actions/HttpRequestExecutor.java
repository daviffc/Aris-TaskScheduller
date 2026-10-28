
package com.taskScheduller.actions;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.enums.ActionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HttpRequestExecutor implements ActionExecutor {

    private final boolean enabled;
    private final Set<String> allowedHosts;

    public HttpRequestExecutor(
            @Value("${aris.http-actions.enabled:false}") boolean enabled,
            @Value("${aris.http-actions.allowed-hosts:}") String hosts
    ) {
        this.enabled = enabled;
        this.allowedHosts = Arrays.stream(hosts.split(","))
                .map(String::trim)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ActionType supports() {
        return ActionType.HTTP_REQUEST;
    }

    @Override
    public void execute(Map<String, String> config) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("HTTP actions are disabled");
        }

        String url = config.get("url");

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("HTTP action requires a URL");
        }

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("Invalid HTTPS destination");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);

        if (!allowedHosts.contains(host)) {
            throw new IllegalArgumentException("Destination is not allowed");
        }

        String method = config.getOrDefault("method", "GET")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!method.equals("GET") && !method.equals("HEAD")) {
            throw new IllegalArgumentException("Unsupported HTTP method");
        }

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(
                        "HTTP request failed with status " + responseCode
                );
            }

            System.out.println(
                    "[HTTP_REQUEST] " + method + " " + host
                            + " -> " + responseCode
            );
        } finally {
            connection.disconnect();
        }
    }
}
