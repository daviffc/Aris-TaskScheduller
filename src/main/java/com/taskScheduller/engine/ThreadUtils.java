package com.taskScheduller.engine;

public final class ThreadUtils {

    private ThreadUtils() {
    }

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}