
package com.taskScheduller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutionPoolConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executionPool() {
        AtomicInteger counter = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                4,
                4,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(
                        runnable,
                        "execution-" + counter.getAndIncrement()
                ),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
