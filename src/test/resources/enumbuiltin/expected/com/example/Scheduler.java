package com.example;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    public static final ScheduledExecutorService ses = null;

    public static void schedule(Runnable task, int millis) {
        ses.schedule(task, millis, TimeUnit.MILLISECONDS);
    }
}
