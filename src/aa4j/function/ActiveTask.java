package aa4j.function;

import aa4j.task.Task;

/**
 * A functional interface represented by a {@link Task} object.
 * @see ActiveCancellableTaskOf
 */
@FunctionalInterface
public interface ActiveTask {
	/**
	 * Functional method of the interface. Will be executed on a thread pool.
	 * If the method returns normally, the task completes successfully.
	 * If the method throws an exception, the task will fail with that exception.
	 * The representing task cannot be cancelled. 
	 * @throws Exception When the task encounters some exception
	 */
	public void runTask() throws Exception;
}
