
package com.taskScheduller.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionPoolConfigTest {

    @Test
    void shouldRejectTasksWhenPoolAndQueueAreFull() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor)
                new ExecutionPoolConfig().executionPool();

        CountDownLatch latch = new CountDownLatch(1);

        try {
            for (int i = 0; i < 104; i++) {
                pool.execute(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            assertEquals(100, pool.getQueue().size());

            assertThrows(
                    RejectedExecutionException.class,
                    () -> pool.execute(() -> {})
            );
        } finally {
            latch.countDown();
            pool.shutdownNow();
        }
    }
}
