package aa4j.task;

/**
 * This enumeration contains possible results for an attempt to synchronize the current
 * thread to a task. 
 */
public enum SyncResult {
	/**
	 * The waiting thread has synchronized with the Task, and the task is done
	 * (successfully, failed with an exception or cancelled) after the method returns.
	 */
	THREAD_SYNCHRONIZED,
	/**
	 * The waiting thread was interrupted while waiting for the task to complete,
	 * and this caused the method to return early.<br>
	 * This does not cause an {@link InterruptedException}, but the current thread's
	 * interrupt flag will be set when this result is returned.
	 */
	THREAD_INTERRUPTED,
	/**
	 * The waiting thread continues before the task is completed because the
	 * specified timeout elapsed.
	 */
	WAIT_TIMEOUT,
	/**
	 * The waiting thread continues before the task is completed because
	 * the synchronization process (and not the task) was cancelled using
	 * the supplied {@link CancellationToken}.
	 */
	WAIT_CANCELLED;
	
	/**
	 * Can be called to throw an {@link InterruptedException} in case the
	 * synchronization result is {@link #THREAD_INTERRUPTED} and the interrupt flag has not yet been cleared by other means
	 * @return {@code this}
	 * @throws InterruptedException When the threads interrupt flag was set
	 */
	public SyncResult interrupt() throws InterruptedException {
		if(this == THREAD_INTERRUPTED && Thread.interrupted())
			throw new InterruptedException("Task synchronization interrupted");
		return this;
	}
	
	/**
	 * Whether the synchronization result indicates that the awaited task is completed in any way.
	 * @return {@code true} for {@link #THREAD_SYNCHRONIZED}, {@code false} for all other enum values
	 */
	public boolean isTaskCompleted() {
		return this == THREAD_SYNCHRONIZED;
	}
}
