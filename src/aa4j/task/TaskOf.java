package aa4j.task;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import aa4j.AA4JStatic;
import aa4j.TaskNotDoneException;

/**
 * An asynchronously executing Task that has a typed result.
 * @param <T> The type of the result
 */
public interface TaskOf<T> {
	
	// waiting methods
	/**
	 * Wait until the task is done or the calling thread is interrupted. 
	 * @return {@code this}
	 * @throws InterruptedException When the thread is interrupted while waiting for the task
	 */
	public TaskOf<T> await() throws InterruptedException;
	/**
	 * Wait until the task is done, ignoring interrupts of the calling thread.<br>
	 * If the thread is interrupted while waiting, the thread's interrupt flag will
	 * be set after this method returns.
	 * @return {@code this}
	 */
	public TaskOf<T> awaitUninterruptibly();
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
	public TaskOf<T> await(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
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
	public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException;
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
	public TaskOf<T> await(CancellationToken token) throws InterruptedException, CancellationException;
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
	public TaskOf<T> awaitUninterruptibly(CancellationToken token) throws CancellationException;
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
	public TaskOf<T> await(long time, TimeUnit unit, CancellationToken token) throws InterruptedException, TimeoutException, CancellationException;
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
	public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token) throws TimeoutException, CancellationException;
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
	
	/**
	 * Whether the task can be cancelled while running.<br>
	 * Cancelling before execution starts is always possible and cancelling after
	 * execution is done has no effect.<br>
	 * Even if a task is marked as cancellable, the associated action can still ignore the cancellation request.
	 * @return {@code true} if the task can be cancelled while running, {@code false} if not
	 */
	public boolean isCancellable();
	
	//Result methods
	/**
	 * Waits until the task is done or the waiting thread is interrupted and returns the
	 * result of the task.<br> 
	 * A shorthand method for calling {@link #await()} and {@link #getResult()}.
	 * @return The result of the task, if the task was successful
	 * @throws ExecutionException When the task has failed with an exception. That exception is
	 * available through {@link ExecutionException#getCause()}.
	 * @throws CancellationException When the task was cancelled before completion
	 * @throws InterruptedException When the calling thread was interrupted while waiting
	 */
	public default T awaitResult() throws ExecutionException, CancellationException, InterruptedException {
		return await().getResult();
	}
	/**
	 * Gets the result of this task only if it is already completed.<br>
	 * A task is guaranteed to be completed after any of the {@code await...} method
	 * returns without throwing an exception, or when {@link #isDone()} is {@code true}.
	 * @return The result of this task, if the task was successful
	 * @throws ExecutionException When the task has failed with an exception. That exception is
	 * available through {@link ExecutionException#getCause()}.
	 * @throws CancellationException When the task was cancelled before completion
	 * @throws TaskNotDoneException When the task is not done
	 */
	public T getResult() throws ExecutionException, CancellationException, TaskNotDoneException;
	/**
	 * Gets the result of this task immediately, or returns an alternate value
	 * when no result is available.
	 * @param value The value to be returned when the task is not yet complete, failed with an exception or cancelled
	 * @return The result of the task, if the task was successful
	 */
	public T getResultOr(T value);
	/**
	 * Gets the result of this task immediately, or returns an alternate value
	 * when no result is available.
	 * @param valueWhenIncomplete The value to be returned when the task is not yet complete
	 * @param valueWhenFailed The value to be returned when the task is failed with an exception
	 * @param valueWhenCancelled The value to be returned when the task was cancelled
	 * @return The result of the task, if the task was successful
	 */
	public T getResultOr(T valueWhenIncomplete, T valueWhenFailed, T valueWhenCancelled);
	
	/**
	 * Gets the result of this task and expect no checked exceptions to be caused by the associated action.<br>
	 * All exceptions that occur in the action will be wrapped in a {@link RuntimeException} and don't
	 * have to be caught.<br>
	 * Unlike {@link #getResult()}, this method does not throw an {@link ExecutionException} because
	 * the action is not supposed to throw any checked exceptions.
	 * @param remainingExs A handler for all exceptions produced by the action.
	 * Most of the time using {@code RuntimeException::new} or the equivalent {@link AA4JStatic#TO_UNCHECKED} is sufficient
	 * @return The result of the task, if the task was successful
	 * @throws CancellationException When the task was cancelled
	 * @throws TaskNotDoneException When the task is not done
	 */
	public T getResult(Function<? super Throwable, ? extends RuntimeException> remainingExs) throws CancellationException, TaskNotDoneException;
	/**
	 * Gets the result of this task and expect one type of (checked) exception to possibly be thrown by the
	 * associated action.<br> 
	 * All other exceptions will be wrapped in a {@link RuntimeException}.<br>
	 * Unlike {@link #getResult()}, this method does not throw an {@link ExecutionException} because
	 * it is only supposed to produce one specific checked exception.
	 * @param <E1> The type of the expected checked exception
	 * @param ex1 The type of the expected checked exception
	 * @param remainingExs A handler for all exceptions of another type produced by the action.
	 * Most of the time using {@code RuntimeException::new} or the equivalent {@link AA4JStatic#TO_UNCHECKED} is sufficient
	 * @return The result of the task, if the task was successful
	 * @throws E1 The exception, if it is thrown by the associated action
	 * @throws CancellationException When the task was cancelled
	 * @throws TaskNotDoneException When the task is not done
	 */
	public <E1 extends Throwable> T getResult(Class<E1> ex1,
			Function<? super Throwable, ? extends RuntimeException> remainingExs) throws E1, CancellationException, TaskNotDoneException;
	/**
	 * Gets the result of this task and expect two types of (checked) exceptions to possibly be thrown by the
	 * associated action.<br> 
	 * All other exceptions will be wrapped in a {@link RuntimeException}.<br>
	 * Unlike {@link #getResult()}, this method does not throw an {@link ExecutionException} because
	 * it is only supposed to produce two specific checked exceptions.
	 * @param <E1> The type of the first expected checked exception
	 * @param <E2> The type of the second expected checked exception
	 * @param ex1 The type of the first expected checked exception
	 * @param ex2 The type of the second expected checked exception
	 * @param remainingExs A handler for all exceptions of another type produced by the action.
	 * Most of the time using {@code RuntimeException::new} or the equivalent {@link AA4JStatic#TO_UNCHECKED} is sufficient
	 * @return The result of the task, if the task was successful
	 * @throws E1 The first exception, if it is thrown by the associated action
	 * @throws E2 The second exception, if it is thrown by the associated action
	 * @throws CancellationException When the task was cancelled
	 * @throws TaskNotDoneException When the task is not done
	 */
	public <E1 extends Throwable, E2 extends Throwable> T getResult(Class<E1> ex1, Class<E2> ex2,
			Function<? super Throwable, ? extends RuntimeException> remainingExs) throws E1, E2, CancellationException, TaskNotDoneException;
	/**
	 * Gets the result of this task and expect three types of (checked) exceptions to possibly be thrown by the
	 * associated action.<br> 
	 * All other exceptions will be wrapped in a {@link RuntimeException}.<br>
	 * Unlike {@link #getResult()}, this method does not throw an {@link ExecutionException} because
	 * it is only supposed to produce three specific checked exceptions.
	 * @param <E1> The type of the first expected checked exception
	 * @param <E2> The type of the second expected checked exception
	 * @param <E3> The type of the third expected checked exception
	 * @param ex1 The type of the first expected checked exception
	 * @param ex2 The type of the second expected checked exception
	 * @param ex3 The type of the thrid expected checked exception
	 * @param remainingExs A handler for all exceptions of another type produced by the action.
	 * Most of the time using {@code RuntimeException::new} or the equivalent {@link AA4JStatic#TO_UNCHECKED} is sufficient
	 * @return The result of the task, if the task was successful
	 * @throws E1 The first exception, if it is thrown by the associated action
	 * @throws E2 The second exception, if it is thrown by the associated action
	 * @throws E3 The third exception, if it is thrown by the associated action
	 * @throws CancellationException When the task was cancelled
	 * @throws TaskNotDoneException When the task is not done
	 */
	public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> T getResult(Class<E1> ex1, Class<E2> ex2, Class<E3> ex3,
			Function<? super Throwable, ? extends RuntimeException> remainingExs) throws E1, E2, E3, CancellationException, TaskNotDoneException;
	
	/**
	 * The result of this task as an {@link Optional}.<br>
	 * The optional will hold the result of the task only if it was successful, in all
	 * other cases (cancelled, failed, not complete) the optional will be empty.
	 * @return An optional with the result if the task was successful, or an empty optional if no result was produced
	 */
	public Optional<T> getResultIfSuccess();
	
	/**
	 * The result of this task as an {@link Optional}.<br>
	 * The optional will hold the result if the task has completed successfully.
	 * If this task is still running, this method returns an empty optional.
	 * If this task is cancelled or failed, the corresponding exception will be thrown
	 * @return An optional with the result, or an empty optional when the task is still ongoing
	 * @throws ExecutionException When the task failed with an exception. That exception will be
	 * set as the ExecutionExceptions caused
	 * @throws CancellationException When the task was cancelled
	 */
	public Optional<T> getResultIfPresent() throws ExecutionException, CancellationException;
	
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
	 *   	If the parameter is {@code false}, It returns {@code false} immediately. The method will only return {@code true}
	 *      if {@link CancelResult#SUCCESSFULLY_CANCELLED} is returned, and {@code false} for all other return values.</td></tr>
	 *   <tr><td>{@link Future#get()}</td><td>{@link #awaitResult()} (which is equivalent to calling
	 *   {@link #await()} and then {@link #getResult()})</td></tr>
	 *   <tr><td>{@link Future#get(long, TimeUnit)}</td><td> {@link #await(long, TimeUnit)} and then {@link #getResult()}</td></tr>
	 * </table>
	 * @return A {@link Future} that is a view of this task
	 */
	public Future<T> future();
	/**
	 * The {@link CompletionStage} that is backing this task.
	 * @return A completion stage for this task
	 */
	public CompletionStage<T> stage();
	/**
	 * A {@link Task} that represents the same action as this task, but ignores the resulting value.
	 * The returned task will be a view of this task, so all states will be synchronized with this ones.
	 * @return A {@link Task} view of this {@link TaskOf}
	 */
	public Task task();
	
	//Handler methods
	/**
	 * Registers an action to be executed when the task completes in any way.<br>
	 * The action will run on the thread that causes the task to complete.
	 * @param action The action that should run when the task is done
	 * @return {@code this}
	 * @see #whenDoneAsync(Runnable)
	 */
	public TaskOf<T> whenDone(Runnable action);
	/**
	 * Registers an action to be executed when the task completes successfully.
	 * The result value of the task will be supplied to the action.<br>
	 * The action will run on the thread that causes the task to succeed.
	 * @param action The action that should run when the task is successful
	 * @return {@code this}
	 * @see #whenSuccessAsync(Consumer)
	 */
	public TaskOf<T> whenSuccess(Consumer<? super T> action);
	/**
	 * Registers an action to be executed when the task fails with an exception.
	 * The exception that caused the task to fail will be supplied to the action.<br>
	 * The action will run on the thread that causes the task to fail.
	 * @param action The action that should run when the task fails
	 * @return {@code this}
	 * @see #whenFailedAsync(Consumer)
	 */
	public TaskOf<T> whenFailed(Consumer<? super Throwable> action);
	/**
	 * Registers an action to be executed when the task is cancelled.<br>
	 * The action will run on the thread that cancels the task.
	 * @param action The action that should run when the task is cancelled
	 * @return {@code this}
	 * @see #whenCancelledAsync(Runnable)
	 */
	public TaskOf<T> whenCancelled(Runnable action);
	
	/**
	 * Registers an action to be executed when the task completes in any way.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to complete.
	 * @param action The action that should run when the task is done
	 * @return {@code this}
	 * @see #whenDone(Runnable)
	 */
	public TaskOf<T> whenDoneAsync(Runnable action);
	/**
	 * Registers an action to be executed when the task completes successfully.
	 * The result value of the task will be supplied to the action.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to succeed.
	 * @param action The action that should run when the task is successful
	 * @return {@code this}
	 * @see #whenSuccess(Consumer)
	 */
	public TaskOf<T> whenSuccessAsync(Consumer<? super T> action);
	/**
	 * Registers an action to be executed when the task fails with an exception.
	 * The exception that caused the task to fail will be supplied to the action.<br>
	 * The action will run on an asynchronous executor and not block the thread that causes the task to fail.
	 * @param action The action that should run when the task fails
	 * @return {@code this}
	 * @see #whenFailed(Consumer)
	 */
	public TaskOf<T> whenFailedAsync(Consumer<? super Throwable> action);
	/**
	 * Registers an action to be executed when the task is cancelled.<br>
	 * The action will run on an asynchronous executor and not block the thread that cancels the task.
	 * @param action The action that should run when the task is cancelled
	 * @return {@code this}
	 * @see #whenCancelled(Runnable)
	 */
	public TaskOf<T> whenCancelledAsync(Runnable action);
}
