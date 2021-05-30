package aa4j.task;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * The {@link TaskAccess} interface provides complete
 * access to all views and objects associated with a task.
 * @param <T> The type of task result value
 */
public interface TaskAccess<T> {

	/**
	 * The typed {@link TaskOf} view of this task.
	 * @return  A {@link TaskOf} for this task
	 */
	public TaskOf<T> taskOf();
	/**
	 * The {@link Task} view of this task.
	 * @return A {@link Task} for this task
	 */
	public Task task();
	/**
	 * A typed {@link TaskCompletionSourceOf} that can complete this task.
	 * @return A {@link TaskCompletionSourceOf} for this task
	 */
	public TaskCompletionSourceOf<T> tcsOf();
	/**
	 * A {@link TaskCompletionSource} that can complete this task (with {@code null}).
	 * @return A {@link TaskCompletionSource} for this task
	 */
	public TaskCompletionSource tcs();
	/**
	 * The {@link CompletionStage} that is backing this task. Not all methods are properly supported.
	 * @return A {@link CompletionStage} for this task
	 */
	public CompletionStage<T> stage();
	/**
	 * A {@link Future} that is a view of this task.
	 * @return A {@link Future} for this task
	 */
	public Future<T> future();
	/**
	 * Creates a new {@link CancellationToken} that might be capable of cancelling this task.
	 * @return A {@link CancellationToken} for this task
	 */
	public CancellationToken newToken();

}
