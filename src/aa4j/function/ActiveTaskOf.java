package aa4j.function;

import aa4j.task.TaskOf;

/**
 * A functional interface represented by a {@link TaskOf} object.
 * @param <T> The return type of the task
 * @see ActiveCancellableTaskOf
 */
@FunctionalInterface
public interface ActiveTaskOf<T> {
	/**
	 * Functional method of the interface. Will be executed on a thread pool.
	 * If the method returns normally, the task completes successfully with the result.
	 * If the method throws an exception, the task will fail with that exception.
	 * The representing task cannot be cancelled. 
	 * @return The result of the task.
	 * @throws Exception When the task encounters some exception
	 */
	public T runTask() throws Exception;
}
