package aa4j.function;

import aa4j.CancellationConfirmed;
import aa4j.CancellationStatusSupplier;
import aa4j.task.Task;

/**
 * A functional interface represented by a {@link Task} object.
 * @see ActiveTaskOf
 */
@FunctionalInterface
public interface ActiveCancellableTask {

	/**
	 * Functional method of the interface. Will be executed on a thread pool.
	 * If the method returns normally, the task completes successfully.
	 * If the method throws an exception, the task will fail with that exception.
	 * The representing task can be cancelled. The implementation is expected to
	 * handle cancellation requests properly and quickly. 
	 * @param tcss Can be used to query whether a cancellation was requested
	 * @throws Exception When the task encounters some exception
	 * @throws CancellationConfirmed Thrown when cancellation is requested and permitted by the executing code
	 */
	public void runTask(CancellationStatusSupplier tcss) throws Exception, CancellationConfirmed;
	
}
