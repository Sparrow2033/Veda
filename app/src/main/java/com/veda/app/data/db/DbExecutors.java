package com.veda.app.data.db;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DbExecutors {
    private static volatile DbExecutors INSTANCE;

    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final Executor mainThread = command -> new Handler(Looper.getMainLooper()).post(command);

    private DbExecutors() {}

    public static DbExecutors get() {
        if (INSTANCE == null) {
            synchronized (DbExecutors.class) {
                if (INSTANCE == null) INSTANCE = new DbExecutors();
            }
        }
        return INSTANCE;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }
}