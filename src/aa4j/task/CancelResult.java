package aa4j.task;

/**
 * This enumeration contains a set of possible results for attempting to cancel a task.
 */
public enum CancelResult {
	/**
	 * The task was still running or not yet started
	 * when the cancellation attempt was made, and the task was
	 * either prevented from running or stopped successfully.
	 * <p>
	 * Note that the ability to cancel a task is a promise made by the implementation of the task,
	 * and that it might take some time after this result is returned until all asynchronous
	 * actions associated with this task have stopped.
	 */
	SUCCESSFULLY_CANCELLED(true),
	/**
	 * The cancellation attempt was unsuccessful because
	 * either the task cannot be cancelled at all, or because the
	 * task was already running and cannot be cancelled while running.
	 */
	UNABLE_TO_CANCEL(false),
	/**
	 * Cancellation of the task was possible and requested,
	 * and the tasks state will switch to cancelled when the cancellation
	 * was confirmed, or to failed/succeeded if one of those happens first
	 */
	CANCELLATION_PENDING(false),
	/**
	 * Cancellation of the task is no longer possible
	 * because it was already cancelled.
	 */
	ALREADY_CANCELLED(true),
	/**
	 * Cancellation of the task is no longer possible
	 * because it has already successfully completed.
	 */
	ALREADY_SUCCEEDED(true),
	/**
	 * Cancellation of the task is no longer possible
	 * beacuse it has already failed with an exception.
	 */
	ALREADY_FAILED(true);
	
	private final boolean stopped;
	
	private CancelResult(boolean stopped) {
		this.stopped = stopped;
	}
	
	/**
	 * The method can be used to determine if the task is stopped
	 * after the cancellation attempt.<br>
	 * This is true for a successful cancellation attempt, but also
	 * if the task was already stopped before the cancellation attempt was made.<br>
	 * A return value of {@code true} assures that the task is no longer executing,
	 * while {@code false} can mean that it is still running.
	 * @return {@code true} if the task is stopped now, {@code false} if it is still running
	 */
	public boolean isTaskStopped() {
		return stopped;
	}
	
}
