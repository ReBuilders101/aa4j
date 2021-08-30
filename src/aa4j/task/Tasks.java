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
	
	public static Task delay(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(new CompletableFuture<>().completeOnTimeout(null, time, unit), null).task();
	}
	
	public static Task delayCancellable(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(new CompletableFuture<>().completeOnTimeout(null, time, unit), AA4JStatic.NOOP).task();
	}
	
}
