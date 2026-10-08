package com.taskScheduller.actions;

import com.taskScheduller.domain.ActionExecutor;
import com.taskScheduller.domain.enums.ActionType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestExecutor implements ActionExecutor {

    @Override
    public ActionType supports() {
        return ActionType.HTTP_REQUEST;
    }

    @Override
    public void execute(Map<String, String> config) throws Exception {
        String url = config.get("url");
        String method = config.getOrDefault("method", "GET");

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("HTTP_REQUEST step requires a 'url' config key");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP request failed with status code: " + responseCode);
            }
            System.out.println("[HTTP_REQUEST] " + method + " " + url + " → " + responseCode);
        } finally {
            connection.disconnect();
        }
    }
}