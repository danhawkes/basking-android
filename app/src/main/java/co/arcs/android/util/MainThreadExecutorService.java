package co.arcs.android.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;

/**
 * Executor service that runs tasks on the Android main thead.
 */
public class MainThreadExecutorService extends HandlerExecutorService {

    private static final MainThreadExecutorService instance = new MainThreadExecutorService();

    private MainThreadExecutorService() {
        super(new Handler(Looper.getMainLooper()));
    }

    public static ExecutorService get() {
        return instance;
    }
}
