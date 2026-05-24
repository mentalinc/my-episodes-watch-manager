package nz.mentalinc.episodeWatcher.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TaskRunner {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static ExecutorService getExecutor() {
        return executor;
    }

    private TaskRunner() {}
}
