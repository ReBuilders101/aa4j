package aa4j;

import aa4j.function.ActiveCancellableTaskOf;
import aa4j.task.Task;

/**
 * This object can be used by the executing side of a task to check
 * whether a cancellation was requested on the representing {@link Task}
 * object.<p>
 * If a task is marked as cancellable, it is expected to always react quickly and
 * to throw the confirmation object returned by {@link #allowCancellation()}.
 */
public interface CancellationStatusSupplier {
	
	/**
	 * Check whether cancellation of this task was requested. If {@code true},
	 * the running task should react as quickly as possible by throwing the confirmation object returned
	 * by {@link #allowCancellation()}.
	 * @return {@code true} if the task was cancelled, {@code false} otherwise
	 */
	public boolean isCancellationRequested();

	/**
	 * Generates a confirmation object which can be thrown from inside the executing {@link ActiveCancellableTaskOf}
	 * to indicate a successful cancellation.
	 * @return A {@link CancellationConfirmed} object (throwable)
	 * @throws CancellationStateException If this method is called when cancellation was <b>not</b> requested
	 */
	public default CancellationConfirmed allowCancellation() throws CancellationStateException {
		if(isCancellationRequested()) {
			return new CancellationConfirmed();
		} else {
			throw new CancellationStateException();
		}
	}
}
