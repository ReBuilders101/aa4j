package aa4j.task;

/**
 * This interface allows arbitrary completion of a task 
 * @param <T> The type of the value for completion
 */
public interface TaskCompletionSourceOf<T> {
	/**
	 * Completes the task with the provided value, if possible.
	 * @param value The result value for the task
	 * @return {@code true} if the call caused the task to succeed, {@code false} if the
	 * task was already done when this method was called.
	 */
	public boolean succeed(T value);
	/**
	 * Fails the task with the provided exception, if possible.
	 * @param exception The exception that causes the task to fail
	 * @return {@code true} if the call caused the task to fail, {@code false}
	 * if the task was already done when this method was called
	 */
	public boolean fail(Throwable exception);
	/**
	 * The task that will be completed by this completer.
	 * @return The task for this completer
	 */
	public TaskOf<T> taskOf();
}
