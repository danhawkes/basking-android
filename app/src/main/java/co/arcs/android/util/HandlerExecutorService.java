package co.arcs.android.util;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;

import com.google.common.util.concurrent.AbstractListeningExecutorService;

/**
 * An executor service that runs tasks on an Android {@link Handler}. This in
 * intended to be used as a bridge between ExecutorService-driven network code
 * and the main thread's {@link Looper}/{@link Handler} setup.
 * <p>
 * Typical use in conjunction with {@link ApiInterfaceImpl} would be as follows:
 * 
 * <pre>
 * ExecutorService exec = new HandlerExecutorService(new Handler(Looper.getMainLooper()));
 * ApiInterface api = ...
 * 
 * // Send the request, and add a listener for the result to be run on main thread
 * Futures.addCallBack(futureResponse, new FutureCallback<Response>() {
 *     
 *     public void onSuccess(Response result) {
 *         // Do something with response on main thread
 *     }
 *     
 * }, exec);
 * </pre>
 * 
 * </p>
 * <p>
 * Source slightly modified from Guava's SameThreadExecutorService.
 * </p>
 */
public class HandlerExecutorService extends AbstractListeningExecutorService {

	private final Handler handler;

	public HandlerExecutorService(Handler handler) {
		this.handler = handler;
	}

	/**
	 * Lock used whenever accessing the state variables (runningTasks, shutdown,
	 * terminationCondition) of the executor
	 */
	private final Lock lock = new ReentrantLock();

	/** Signalled after the executor is shutdown and running tasks are done */
	private final Condition termination = lock.newCondition();

	/*
	 * Conceptually, these two variables describe the executor being in one of
	 * three states: - Active: shutdown == false - Shutdown: runningTasks > 0
	 * and shutdown == true - Terminated: runningTasks == 0 and shutdown == true
	 */
	private int runningTasks = 0;
	private boolean shutdown = false;

	@Override
	public void execute(Runnable command) {
		startTask();
		try {
			handler.post(command);
		} finally {
			endTask();
		}
	}

	@Override
	public boolean isShutdown() {
		lock.lock();
		try {
			return shutdown;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void shutdown() {
		lock.lock();
		try {
			shutdown = true;
		} finally {
			lock.unlock();
		}
	}

	// See sameThreadExecutor javadoc for unusual behavior of this method.
	@Override
	public List<Runnable> shutdownNow() {
		shutdown();
		return Collections.emptyList();
	}

	@Override
	public boolean isTerminated() {
		lock.lock();
		try {
			return shutdown && (runningTasks == 0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		lock.lock();
		try {
			for (;;) {
				if (isTerminated()) {
					return true;
				} else if (nanos <= 0) {
					return false;
				} else {
					nanos = termination.awaitNanos(nanos);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Checks if the executor has been shut down and increments the running task
	 * count.
	 * 
	 * @throws RejectedExecutionException
	 *             if the executor has been previously shutdown
	 */
	private void startTask() {
		lock.lock();
		try {
			if (isShutdown()) {
				throw new RejectedExecutionException("Executor already shutdown");
			}
			runningTasks++;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Decrements the running task count.
	 */
	private void endTask() {
		lock.lock();
		try {
			runningTasks--;
			if (isTerminated()) {
				termination.signalAll();
			}
		} finally {
			lock.unlock();
		}
	}

}
