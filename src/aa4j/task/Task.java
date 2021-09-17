package aa4j.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import aa4j.TaskNotDoneException;

/**
 * An asynchonously executing task without a result
 */
public interface Task {
	
	/**
	 * Wait until the task is done or the calling thread is interrupted. 
	 * @return {@code this}
	 * @throws InterruptedException When the thread is interrupted while waiting for the task
	 */
	public Task await() throws InterruptedException;
	/**
	 * Wait until the task is done, ignoring interrupts of the calling thread.<br>
	 * If the thread is interrupted while waiting, the thread's interrupt flag will
	 * be set after this method returns.
	 * @return {@code this}
	 */
	public Task awaitUninterruptibly();
	/**
	 * Waits until the task is done, the timeout has elapsed or the calling thread is interrupted.
	 * @param time The time to wait
	 * @param unit The unit of the {@code time} value
	 * @return {@code this}
	 * @throws InterruptedException When the thread is interrupted while waiting for the task 
	 * @throws TimeoutException When the timeout elapsed before the task was done
	 * @throws IllegalArgumentException When {@code time} is negative
	 * @throws NullPointerException When {@code unit} is {@code null}
	 */
	public Task await(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
	/**
	 * Waits until the task is done or the timeout has elapsed.<br>
	 * If the thread is interrupted while waiting, the thread's interrupt flag will
	 * be set after this method returns.
	 * @param time The time to wait
	 * @param unit The unit of the {@code time} value
	 * @return {@code this} 
	 * @throws TimeoutException When the timeout elapsed before the task was done
	 * @throws IllegalArgumentException When {@code time} is negative
	 * @throws NullPointerException When {@code unit} is {@code null}
	 */
	public Task awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException;
	/**
	 * Waits until the task is done or the supplied token requests cancellation, or the calling thread is interrupted.<br>
	 * Cancelling the token will cancel the waiting process and resume this thread, but will not cancel the task itself.
	 * The cancellation of the token is guaranteed to succeed.
	 * @param token An unbound {@link CancellationToken}
	 * @return {@code this}
	 * @throws InterruptedException When the thread is interrupted while waiting for the task
	 * @throws CancellationException When the token was cancelled before the task was done
	 * @throws IllegalArgumentException When the token is already bound to an action
	 * @throws NullPointerException When {@code token} is {@code null}
	 */
	public Task await(CancellationToken token) throws InterruptedException, CancellationException;
	/**
	 * Waits until the task is done or the supplied token requests cancellation.<br>
	 * Cancelling the token will cancel the waiting process and resume this thread, but will not cancel the task itself.
	 * The cancellation of the token is guaranteed to succeed.<br>
	 * If the thread is interrupted while waiting, the thread's interrupt flag will
	 * be set after this method returns.
	 * @param token An unbound {@link CancellationException}
	 * @return {@code this}
	 * @throws CancellationException When the token was cancelled before the task was done
	 * @throws IllegalArgumentException When the token is already bound to an action
	 * @throws NullPointerException When {@code token} is {@code null}
	 */
	public Task awaitUninterruptibly(CancellationToken token) throws CancellationException;
	/**
	 * Waits until the task is done, or the timeout has elapsed, or the supplied token
	 * requests cancellation, or the calling thread is interrupted.<br>
	 * Cancelling the token will cancel the waiting process and resume this thread, but will not cancel the task itself.
	 * The cancellation of the token is guaranteed to succeed.
	 * @param time The time to wait
	 * @param unit The unit of the {@code time} value
	 * @param token An unbound {@link CancellationToken}
	 * @return {@code this}
	 * @throws InterruptedException When the thread is interrupted while waiting for the task
	 * @throws TimeoutException When the timeout elapsed before the task was done
	 * @throws CancellationException When the token was cancelled before the task was done
	 * @throws IllegalArgumentException When {@code time} is negative or when the token is already bound to an action
	 * @throws NullPointerException When {@code token} or {@code unit} is {@code null}
	 */
	public Task await(long time, TimeUnit unit, CancellationToken token) throws InterruptedException, TimeoutException, CancellationException;
	/**
	 * Waits until the task is done, or the timeout has elapsed, or the supplied token
	 * requests cancellation.<br>
	 * Cancelling the token will cancel the waiting process and resume this thread, but will not cancel the task itself.
	 * The cancellation of the token is guaranteed to succeed.<br>
	 * If the thread is interrupted while waiting, the thread's interrupt flag will
	 * be set after this method returns.
	 * @param time The time to wait
	 * @param unit The unit of the {@code time} value
	 * @param token An unbound {@link CancellationToken}
	 * @return {@code this}
	 * @throws TimeoutException When the timeout elapsed before the task was done
	 * @throws CancellationException When the token was cancelled before the task was done
	 * @throws IllegalArgumentException When {@code time} is negative or when the token is already bound to an action
	 * @throws NullPointerException When {@code token} or {@code unit} is {@code null}
	 */
	public Task awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token) throws TimeoutException, CancellationException;
	/**
	 * Synchronizes the calling thread with the completion of the task.<br>
	 * This method provides the same features as the {@code await...} methods, but returns
	 * a result value instead of throwing various exceptions.
	 * <p>
	 * Using a timeout, cancellation token and reacting to interrupts is configurable with the parameters:<br>
	 * If {@code unit} is {@code null} or the {@code time} value is negative, no timeout will be set.
	 * If {@code token} is {@code null}, the waiting cannot be cancelled by any token.
	 * </p>
	 * @param time The time to wait
	 * @param unit The unit of the {@code time} value, or {@code null} to not use a timeout
	 * @param token An unbound {@link CancellationToken}, or {@code null} to not use a token
	 * @param interruptible {@code true} if the method should return when the thread is interrupted, {@code false}
	 * if interrupts should be ignored
	 * @return A {@link SyncResult} that describes the reason why this method has returned
	 * @throws IllegalArgumentException When the token is not {@code null} and bound to another action
	 */
	public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible);
	
	/**
	 * Attempts to cancel the execution of this task. Not all task types support cancellation of the
	 * associated action while running.
	 * @return A {@link CancelResult} with details about success or failure of the cancellation
	 */
	public CancelResult cancel();
	
	/**
	 * Does nothing if the task is successfully completed. <br>
	 * If not, it will throw the same exceptions as calling {@link TaskOf#getResult()}
	 * @return {@code this}
	 * @throws ExecutionException When the task has failed with an exception. That exception is
	 * available through {@link ExecutionException#getCause()}.
	 * @throws CancellationException When the task was cancelled before completion
	 * @throws TaskNotDoneException When the task is not done
	 */
	public Task checkSuccess() throws ExecutionException, CancellationException, TaskNotDoneException;
	
	/**
	 * Waits until the task is done or the waiting thread is interrupted and returns normally
	 * if the task was successful.<br> 
	 * A shorthand method for calling {@link #await()} and {@link #checkSuccess()}.
	 * @return {@code this}
	 * @throws ExecutionException When the task has failed with an exception. That exception is
	 * available through {@link ExecutionException#getCause()}.
	 * @throws CancellationException When the task was cancelled before completion
	 * @throws InterruptedException When the calling thread was interrupted while waiting
	 */
	public Task awaitSuccess() throws InterruptedException, ExecutionException, CancellationException;
	
	//state methods
	
	/**
	 * The current state of this task.
	 * Most methods of {@link TaskState} are also directly available on the {@link TaskOf} itself.
	 * <p>
	 * The task state can change at any time according to the rules in {@link TaskState#phase()}.
	 * This means that states of running and initialized might be immediately outdated after this
	 * method returns, but once one of the success, failure or cancelled states is returned, the
	 * state will no longer change.
	 * @return the state of this task.
	 */
	public TaskState getState();
	/**
	 * This method will return {@code true} only if the task is currently running,
	 * not if it is initialized or done.
	 * @return {@code true} if the task is currently running, {@code false} if not
	 * @see TaskState#isRunning()
	 */
	public default boolean isRunning() {
		return getState().isRunning();
	}
	/**
	 * This method will return {@code true} if the task has completed in any way,
	 * successfully, by failure or by cancellation.
	 * @return {@code true} if the task is done in any way, {@code false} if not
	 * @see TaskState#isDone()
	 */
	public default boolean isDone() {
		return getState().isDone();
	}
	/**
	 * This method will return {@code true} only if the task is done and has completed successfully.
	 * @return {@code true} if the task was successful, {@code false} otherwise
	 * @see TaskState#isSuccess()
	 */
	public default boolean isSuccess() {
		return getState().isSuccess();
	}
	/**
	 * This method will return {@code true} only if the task was cancelled before successful completion.
	 * @return {@code true} if the task was cancelled, {@code false} otherwise
	 * @see TaskState#isCancelled()
	 */
	public default boolean isCancelled() {
		return getState().isCancelled();
	}
	/**
	 * This method will return {@code true} only if the task has failed with an exception.
	 * @return {@code true} if the task has failed, {@code false} otherwise
	 * @see TaskState#isFailed()
	 */
	public default boolean isFailed() {
		return getState().isFailed();
	}
	
	//Future-Like access
	/**
	 * Provides a view of this task as a {@link Future}. The methods of that interface map to methods
	 * of this task as follows:
	 * <p>
	 * <table>
	 *   <tr><th>Method in {@link Future}</th><th>Method in {@link TaskOf}</th></tr>
	 *   <tr><td>{@link Future#isCancelled()}</td><td>{@link #isCancelled()}</td></tr>
	 *   <tr><td>{@link Future#isDone()}</td><td>{@link #isDone()}</td></tr>
	 *   <tr><td>{@link Future#cancel(boolean)}</td><td>If the parameter is {@code true}, it calls {@link #cancel()}.
	 *   	If the parameter is {@code false}, it returns {@code false} immediately. The method will only return {@code true}
	 *      if {@link CancelResult#SUCCESSFULLY_CANCELLED} is returned, and {@code false} for all other return values.</td></tr>
	 *   <tr><td>{@link Future#get()}</td><td>{@link #await()} and return {@code null} </td></tr>
	 *   <tr><td>{@link Future#get(long, TimeUnit)}</td><td> {@link #await(long, TimeUnit)} and return {@code null}</td></tr>
	 * </table>
	 * @return A {@link Future} that is a view of this task
	 */
	public Future<?> future();
	/**
	 * The {@link CompletionStage} that is backing this task.
	 * @return A completion stage for this task
	 */
	public CompletionStage<?> stage();
	/**
	 * A {@link TaskOf} that represents the same action as this task, with type {@link Void} and {@code null} as return value
	 * The returned task will be a view of this task, so all states will be synchronized with this ones.
	 * @return A {@link TaskOf} view of this {@link Task}
	 */
	public TaskOf<?> taskOf();
	
	//Handler methods
	/**
	 * Registers an action to be executed when the task completes in any way.<br>
	 * The action will run on the thread that causes the task to complete.
	 * @param action The action that should run when the task is done
	 * @return {@code this}
	 * @see #whenDoneAsync(Runnable)
	 */
	public Task whenDone(Runnable action);
	/**
	 * Registers an action to be executed when the task completes successfully.<br>
	 * The action will run on the thread that causes the task to succeed.
	 * @param action The action that should run when the task is successful
	 * @return {@code this}
	 * @see #whenSuccessAsync(Runnable)
	 */
	public Task whenSuccess(Runnable action);
	/**
	 * Registers an action to be executed when the task fails with an exception.
	 * The exception that caused the task to fail will be supplied to the action.<br>
	 * The action will run on the thread that causes the task to fail.
	 * @param action The action that should run when the task fails
	 * @return {@code this}
	 * @see #whenFailedAsync(Consumer)
	 */
	public Task whenFailed(Consumer<? super Throwable> action);
	/**
	 * Registers an action to be executed when the task is cancelled.<br>
	 * The action will run on the thread that cancels the task.
	 * @param action The action that should run when the task is cancelled
	 * @return {@code this}
	 * @see #whenCancelledAsync(Runnable)
	 */
	public Task whenCancelled(Runnable action);
	
	/**
	 * Registers an action to be executed when the task completes in any way.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to complete.
	 * @param action The action that should run when the task is done
	 * @return {@code this}
	 * @see #whenDone(Runnable)
	 */
	public Task whenDoneAsync(Runnable action);
	/**
	 * Registers an action to be executed when the task completes successfully.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to succeed.
	 * @param action The action that should run when the task is successful
	 * @return {@code this}
	 * @see #whenSuccess(Runnable)
	 */
	public Task whenSuccessAsync(Runnable action);
	/**
	 * Registers an action to be executed when the task fails with an exception.
	 * The exception that caused the task to fail will be supplied to the action.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to fail.
	 * @param action The action that should run when the task fails
	 * @return {@code this}
	 * @see #whenFailed(Consumer)
	 */
	public Task whenFailedAsync(Consumer<? super Throwable> action);
	/**
	 * Registers an action to be executed when the task is cancelled.<br>
	 * The action will run on an asynchronous executor and not block the thread that cancels the task.
	 * @param action The action that should run when the task is cancelled
	 * @return {@code this}
	 * @see #whenCancelled(Runnable)
	 */
	public Task whenCancelledAsync(Runnable action);
}
