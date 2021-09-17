package aa4j;

import java.util.function.Function;
import java.util.function.Supplier;

import aa4j.task.TaskOf;

/**
 * Contains the keyword functions used by aa4j.
 */
public final class AA4JStatic {
	private AA4JStatic() { throw new RuntimeException("No instance for you"); }
	
	public static <T> OnAwaitActions<T> await(TaskOf<T> task) {
		return new AwaitStage<>(task);
	}
	
	public static <T> OnSyncActions<T> sync(Supplier<T> result) {
		return new SyncStage<>(result);
	}
	
	/**
	 * Wraps any type of {@link Throwable} in a {@link RuntimeException}.
	 */
	public static final Function<Throwable, RuntimeException> TO_UNCHECKED = RuntimeException::new;
	/**
	 * Does nothing.
	 */
	public static final Runnable NOOP = () -> {};

}
