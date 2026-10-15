package com.taskScheduller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutionPoolConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executionPool() {
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(4,
                runnable -> new Thread(runnable, "execution-" + counter.getAndIncrement()));
    }
}