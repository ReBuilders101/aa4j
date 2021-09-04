package aa4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import aa4j.function.ActiveCancellableTask;
import aa4j.function.ActiveCancellableTaskOf;
import aa4j.function.ActiveTask;
import aa4j.function.ActiveTaskOf;
import aa4j.task.Task;
import aa4j.task.TaskOf;

/**
 * Can be used similar to an {@link ExecutorService}, but produces
 * {@link TaskOf}-objects instead of {@link Future}s. 
 */
public interface TaskExecutorService extends Executor {

	/**
	 * Initiates a shutdown for this executor. Previously submitted tasks are still executed, but no
	 * new tasks will be accepted.
	 * <p>
	 * Use {@link #awaitTermination(long, TimeUnit)} or {@link #getTerminationTask()} to
	 * wait for all ongoing tasks to complete.
	 * </p>
	 */
	public void shutdown();
	
	
	/**
	 * Initiates a shutdown for this executor. No new tasks will be accepted. All
	 * tasks that are currently executing will be cancelled if possible.
	 * Not all tasks react to cancellation attempts
	 * <p>
	 * Use {@link #awaitTermination(long, TimeUnit)} or {@link #getTerminationTask()} to
	 * wait for all ongoing tasks to complete.
	 * </p>
	 * @return The amount of tasks that were still ongoing and had to be cancelled
	 */
	public int shutdownNow();
	
	/**
	 * Returns {@code true} if the executor has been shut down. In that case, all attempts to submit
	 * a new task will result in a {@link RejectedExecutionException}.
	 * @return {@code true} if the executor is shutdown, {@code false} otherwise
	 */
	public boolean isShutdown();
	
	/** 
	 * Returns {@code true} if all tasks that were submitted to this executor have finished.
	 * Can only ever be {@code true} after {@link #shutdown()} has been called.
	 * @return {@code true} if all tasks have finished, {@code false} if
	 * not shutdown or if tasks are still ongoing
	 */
	public boolean isTerminated();
	
	/**
	 * A {@link Task} that completes when the shutdown procedure is complete and
	 * not tasks are running anymore.
	 * @return A {@link Task} that completes when the executor is terminated successfully
	 */
	public Task getTerminationTask();
	
	/**
	 * Blocks until all tasks have completed execution after a shutdown
	 * request, or the timeout occurs, or the current thread is
	 * interrupted, whichever happens first.
	 * @param timeout The maximum time to wait
	 * @param unit The unit for the timeout
	 * @throws InterruptedException When the current thread is interrupted while waiting
	 * @throws TimeoutException When the timeout expires before termination completes
	 */
	public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
	
	/**
	 * Submits a task for execution.
	 * @param task The task to run using this executor
	 * @return A {@link Task} object representing the running task
	 */
	public Task submit(ActiveTask task);
	
	/**
	 * Submits a cancellable task for execution.
	 * @param task The task to run using this executor
	 * @return A {@link Task} object representing the running task
	 */
	public Task submit(ActiveCancellableTask task);
	
	/**
	 * Submits a task for execution.
	 * @param <T> The result type of the task
	 * @param task The task to run using this executor
	 * @return A {@link TaskOf} object representing the running task
	 */
	public <T> TaskOf<T> submit(ActiveTaskOf<T> task);
	
	/**
	 * Submits a cancellable task for execution.
	 * @param <T> The result type of the task
	 * @param task The task to run using this executor
	 * @return A {@link TaskOf} object representing the running task
	 */
	public <T> TaskOf<T> submit(ActiveCancellableTaskOf<T> task);
}
