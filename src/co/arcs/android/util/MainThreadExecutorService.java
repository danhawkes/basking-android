package co.arcs.android.util;


import java.util.concurrent.ExecutorService;

import android.os.Handler;
import android.os.Looper;

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
