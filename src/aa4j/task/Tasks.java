package aa4j.task;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import aa4j.AA4JStatic;

/**
 * Provides static methods to create different kinds of tasks.
 */
public final class Tasks {
	private Tasks() { throw new RuntimeException("No instance for you"); }
	
	/**
	 * A completely configurable task that represents some internal time-consuming action
	 * (such as connecting a socket). Cannot be cancelled.
	 * @param <T> The result type of the task
	 * @return A {@link TaskAccess} containing all representations of the task
	 */
	public static <T> TaskAccess<T> manualBlocking() {
		return new NonBlockingTask<>(new CompletableFuture<>(), null);
	}
	
	/**
	 * A completely configurable task that represents some internal time-consuming action
	 * (such as connecting a socket). Can be cancelled while running.
	 * @param <T> The result type of the task
	 * @param cancellationHandler The method to invoke when cancellation is requested. It is assumed that 
	 * this cancellation is always successful.
	 * @return A {@link TaskAccess} containing all representations of the task
	 * @throws NullPointerException When {@code cancellationHandler} is {@code null}
	 */
	public static <T> TaskAccess<T> manualBlocking(Runnable cancellationHandler) {
		Objects.requireNonNull(cancellationHandler, "'cancellationHandler' parameter must not be null");
		return new NonBlockingTask<>(new CompletableFuture<>(), cancellationHandler);
	}
	
	/**
	 * Creates a task that completes after a set delay. CAnnot be canelled.
	 * @param time The timespan after which the task completes
	 * @param unit The unit for the time parameter
	 * @return A task that completes after a timeout
	 */
	public static Task delay(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(new CompletableFuture<>().completeOnTimeout(null, time, unit), null).task();
	}
	
	/**
	 * Creates a task that completes after a set delay. CAnnot be canelled.
	 * @param time The timespan after which the task completes
	 * @param unit The unit for the time parameter
	 * @return A task that completes after a timeout
	 */
	public static Task delayCancellable(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(new CompletableFuture<>().completeOnTimeout(null, time, unit), AA4JStatic.NOOP).task();
	}
	
	/**
	 * A task without result that is always completed successfully.
	 * Returns an immutable singleton instance.
	 * @return A completed task
	 */
	public static Task completed() {
		return CompletedTasks.success_untyped;
	}
	
	/**
	 * A successfully completed task with a result.
	 * @param <T> The type of the task result
	 * @param value The value that the task should contain
	 * @return A task that is successfully completed with the given value
	 */
	public static <T> TaskOf<T> success(T value) {
		return CompletedTasks.success(value);
	}
	
	/**
	 * A task that has failed with a exception.
	 * @param <T> The type of the task result
	 * @param failureReason The exception that caused the task to fail
	 * @return A task that has failed with an exception
	 */
	public static <T> TaskOf<T> failure(Throwable failureReason) {
		return CompletedTasks.failure(failureReason);
	}
	
	/**
	 * A task that has failed with a exception.
	 * @param <T> The type of the task result
	 * @param failureReason The exception that caused the task to fail
	 * @param resultType The type of the task result
	 * @return A task that has failed with an exception
	 */
	public static <T> TaskOf<T> failure(Throwable failureReason, Class<T> resultType) {
		return CompletedTasks.failure(failureReason);
	}
	
	/**
	 * A task that is cancelled.
	 * @param <T> The type of the task result
	 * @return A task that has been cancelled
	 */
	public static <T> TaskOf<T> cancelled() {
		return CompletedTasks.cancelled();
	}
	
	/**
	 * A task that is cancelled.
	 * @param <T> The type of the task result
	 * @param resultType The type of the task result
	 * @return A task that has been cancelled
	 */
	public static <T> TaskOf<T> cancelled(Class<T> resultType) {
		return CompletedTasks.cancelled();
	}
	
}
