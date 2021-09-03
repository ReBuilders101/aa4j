package aa4j.task;

/**
 * This interface allows arbitrary completion of a task 
 */
public interface TaskCompletionSource {
	/**
	 * Completes the task successfully.
	 * @return {@code true} if the call caused the task to succeed, {@code false} if the
	 * task was already done when this method was called.
	 */
	public boolean succeed();
	/**
	 * Fails the task with the provided exception, if possible.
	 * @param exception The exception that causes the task to fail
	 * @return {@code true} if the call caused the task to fail, {@code false}
	 * if the task was already done when this method was called
	 */
	public boolean fail(Exception exception);
	/**
	 * The task that will be completed by this completer.
	 * @return The task for this completer
	 */
	public Task task();
	
}
