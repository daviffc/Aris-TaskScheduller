package com.taskScheduller.actions;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.enums.ActionType;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.util.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    private final DnsResolver dnsResolver;

    @Autowired
    public HttpRequestExecutor(
            @Value("${aris.http-actions.enabled:false}") boolean enabled,
            @Value("${aris.http-actions.allowed-hosts:}") String hosts) {
        this(enabled, hosts, new PublicAddressDnsResolver());
    }

    HttpRequestExecutor(
            boolean enabled,
            String hosts,
            DnsResolver dnsResolver) {
        this.enabled = enabled;
        this.allowedHosts = Arrays.stream(hosts.split(","))
                .map(String::trim)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.dnsResolver = dnsResolver;
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
            throw new IllegalArgumentException(
                    "HTTP action requires a URL"
            );
        }

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException(
                    "Invalid HTTPS destination"
            );
        }

        String host = uri.getHost();

        host = host.toLowerCase(Locale.ROOT);

        if (!allowedHosts.contains(host)) {
            throw new IllegalArgumentException(
                    "Destination is not allowed"
            );
        }

        String method = config.getOrDefault("method", "GET")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!method.equals("GET") && !method.equals("HEAD")) {
            throw new IllegalArgumentException(
                    "Unsupported HTTP method"
            );
        }

        ClassicHttpRequest request = method.equals("GET")
                ? new HttpGet(uri)
                : new HttpHead(uri);

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(dnsResolver)
                        .setDefaultConnectionConfig(connectionConfig)
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(5))
                .setRedirectsEnabled(false)
                .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .build()) {

            try (ClassicHttpResponse response =
                         client.executeOpen(null, request, null)) {
                int responseCode = response.getCode();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException(
                            "HTTP request failed with status " + responseCode
                    );
                }

                System.out.println(
                        "[HTTP_REQUEST] " + method + " " + host
                                + " -> " + responseCode
                );
            }
        }
    }
}
