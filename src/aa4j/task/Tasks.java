package aa4j.task;

import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import aa4j.TaskExecutorService;
import aa4j.TaskNotDoneException;
import aa4j.function.ActiveCancellableTask;
import aa4j.function.ActiveCancellableTaskOf;
import aa4j.function.ActiveTask;
import aa4j.function.ActiveTaskOf;

/**
 * Provides static methods to create different kinds of tasks.
 */
public final class Tasks {
	private Tasks() { throw new RuntimeException("No instance for you"); }
	
	/**
	 * A completely configurable task that represents some internal time-consuming action
	 * (such as connecting a socket). Cannot be cancelled.
	 * @param <T> The result type of the task
	 * @return A {@link TaskAccess} containing all representations of the task
	 */
	public static <T> TaskAccess<T> create() {
		return new NonBlockingTask<>(newCpf(), false);
	}
	
	/**
	 * A completely configurable task that represents some internal time-consuming action
	 * (such as connecting a socket). Cannot be cancelled.
	 * @param <T> The result type of the task
	 * @param stage Any {@link CompletionStage} that interacts with {@link CompletableFuture}.
	 * The task will share behavior with the completion stage.
	 * @param canCancel Whether the task can be cancelled
	 * @return A {@link TaskAccess} containing all representations of the task
	 */
	public static <T> TaskAccess<T> create(CompletionStage<T> stage, boolean canCancel) {
		return new NonBlockingTask<>(Objects.requireNonNull(stage), canCancel);
	}
	
	/**
	 * A completely configurable task that represents some internal time-consuming action
	 * (such as connecting a socket). Can be cancelled.
	 * @param <T> The result type of the task
	 * @param onCancellation Handles a cancellation attempt
	 * @return A {@link TaskAccess} containing all representations of the task
	 */
	public static <T> TaskAccess<T> create(Runnable onCancellation) {
		if(onCancellation == null) {
			return new NonBlockingTask<>(newCpf(), true);
		} else {
			return new NonBlockingTask<>(newCpf(), onCancellation);
		}
	}
	
	/**
	 * Creates a task that completes after a set delay. Cannot be canelled.
	 * @param time The timespan after which the task completes
	 * @param unit The unit for the time parameter
	 * @return A task that completes after a timeout
	 */
	public static Task delay(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(newCpf().completeOnTimeout(null, time, unit), false).task();
	}
	
	/**
	 * Creates a task that completes after a set delay. Can be canelled.
	 * @param time The timespan after which the task completes
	 * @param unit The unit for the time parameter
	 * @return A task that completes after a timeout
	 */
	public static Task delayCancellable(long time, TimeUnit unit) {
		if(time < 0) throw new IllegalArgumentException("time cannot be negative");
		Objects.requireNonNull(unit, "'unit' parameter must not be null");
		return new NonBlockingTask<>(newCpf().completeOnTimeout(null, time, unit), true).task();
	}
	
	/**
	 * A task without result that is always completed successfully.
	 * Returns an immutable singleton instance.
	 * @return A completed task
	 */
	public static Task completed() {
		return CompletedTask.success_untyped;
	}
	
	/**
	 * A successfully completed task with a result.
	 * @param <T> The type of the task result
	 * @param value The value that the task should contain
	 * @return A task that is successfully completed with the given value
	 */
	public static <T> TaskOf<T> success(T value) {
		return new CompletedTask<>(value, null, TaskState.SUCCEEDED).taskOfView;
	}
	
	/**
	 * A task that has failed with a exception.
	 * @param <T> The type of the task result
	 * @param failureReason The exception that caused the task to fail
	 * @return A task that has failed with an exception
	 */
	public static <T> TaskOf<T> failure(Exception failureReason) {
		return new CompletedTask<>((T) null, Objects.requireNonNull(failureReason), TaskState.FAILED).taskOfView;
	}
	
	/**
	 * A task that is cancelled.
	 * @param <T> The type of the task result
	 * @return A task that has been cancelled
	 */
	public static <T> TaskOf<T> cancelled() {
		return new CompletedTask<>((T) null, null, TaskState.CANCELLED).taskOfView;
	}
	
	
	
	/**
	 * Creates a Task that represents the {@link ActiveTask} running on a different thread.
	 * Will use the {@link ForkJoinPool#commonPool()} for execution.
	 * @param task The task to run on the executor thread
	 * @return A {@link Task} object representing this task
	 */
	public static Task run(ActiveTask task) {
		return run(task, defaultExecutor());
	}
	
	/**
	 * Creates a Task that represents the {@link ActiveTask} running on a different thread.
	 * @param task The task to run on the executor thread
	 * @param executor The {@link Executor} that determines the thread to run on
	 * @return A {@link Task} object representing this task
	 */
	public static Task run(ActiveTask task, Executor executor) {
		final BlockingTask<?> t = new BlockingTask<>(newCpf(), false);
		final TaskDriver<?> d = new TaskDriver<>(t, task);
		executor.execute(d);
		return t.taskView;
	}
	
	/**
	 * Creates a cancellable Task that represents the {@link ActiveCancellableTask} running on a different thread.
	 * Will use the {@link ForkJoinPool#commonPool()} for execution.
	 * @param task The task to run on the executor thread
	 * @return A {@link Task} object representing this task
	 */
	public static Task run(ActiveCancellableTask task) {
		return run(task, defaultExecutor());
	}
	
	/**
	 * Creates cancellable a Task that represents the {@link ActiveCancellableTask} running on a different thread.
	 * @param task The task to run on the executor thread
	 * @param executor The {@link Executor} that determines the thread to run on
	 * @return A {@link Task} object representing this task
	 */
	public static Task run(ActiveCancellableTask task, Executor executor) {
		final BlockingTask<?> t = new BlockingTask<>(newCpf(), true);
		final TaskDriver<?> d = new TaskDriver<>(t, task);
		executor.execute(d);
		return t.taskView;
	}
	
	/**
	 * Creates a Task that represents the {@link ActiveTaskOf} running on a different thread.
	 * Will use the {@link ForkJoinPool#commonPool()} for execution.
	 * @param <T> The result type of the task
	 * @param task The task to run on the executor thread
	 * @return A {@link TaskOf} object representing this task
	 */
	public static <T> TaskOf<T> run(ActiveTaskOf<T> task) {
		return run(task, defaultExecutor());
	}
	
	/**
	 * Creates a Task that represents the {@link ActiveTaskOf} running on a different thread.
	 * @param <T> The result type of the task
	 * @param task The task to run on the executor thread
	 * @param executor The {@link Executor} that determines the thread to run on
	 * @return A {@link TaskOf} object representing this task
	 */
	public static <T> TaskOf<T> run(ActiveTaskOf<T> task, Executor executor) {
		final BlockingTask<T> t = new BlockingTask<>(newCpf(), false);
		final TaskDriver<T> d = new TaskDriver<>(t, task);
		executor.execute(d);
		return t.taskOfView;
	}
	
	/**
	 * Creates a cancellable Task that represents the {@link ActiveCancellableTaskOf} running on a different thread.
	 * Will use the {@link ForkJoinPool#commonPool()} for execution.
	 * @param <T> The result type of the task
	 * @param task The task to run on the executor thread
	 * @return A {@link TaskOf} object representing this task
	 */
	public static <T> TaskOf<T> run(ActiveCancellableTaskOf<T> task) {
		return run(task, defaultExecutor());
	}
	
	/**
	 * Creates cancellable a Task that represents the {@link ActiveCancellableTaskOf} running on a different thread.
	 * @param <T> The result type of the task
	 * @param task The task to run on the executor thread
	 * @param executor The {@link Executor} that determines the thread to run on
	 * @return A {@link TaskOf} object representing this task
	 */
	public static <T> TaskOf<T> run(ActiveCancellableTaskOf<T> task, Executor executor) {
		final BlockingTask<T> t = new BlockingTask<>(newCpf(), true);
		final TaskDriver<T> d = new TaskDriver<>(t, task);
		executor.execute(d);
		return t.taskOfView;
	}
	
	/**
	 * Maps the result of this task to a different type using a non-blocking mapping function.
	 * @param <T> Type of the existing task
	 * @param <R> Type of the mapped result
	 * @param task The existing task that should be mapped
	 * @param mapFunc A function that can map the task result to the required type
	 * @return A task that completes after the existing task and the mapping function.
	 * In case of cancellation or failure, the returend task will have the same state.
	 */
	public static <T,R> TaskOf<R> map(TaskOf<T> task, Function<T, R> mapFunc) {
		Objects.requireNonNull(task, "'task' parameter must not be null");
		Objects.requireNonNull(mapFunc, "'mapFunc' parameter must not be null");
		return new MappedTask<>(task.stage().thenApply(mapFunc), task::cancel).taskOfView;
	}
	
	public static <T,R> TaskOf<R> chain(TaskOf<T> task, Function<T, TaskOf<R>> chainedTask) {
		Objects.requireNonNull(task, "'task' parameter must not be null");
		Objects.requireNonNull(chainedTask, "'chainedTask' parameter must not be null");
		return ChainedTask.create(task, chainedTask).taskOfView;
	}
	
	//erasure means we will have to have some chain2 function names
	
	public static <R> TaskOf<R> chain(Task task, Supplier<TaskOf<R>> chainedTask) {
		return chain(task.taskOf(), _null -> chainedTask.get());
	}
	
	public static <T> Task chain2(TaskOf<T> task, Function<T, Task> chainedTask) {
		return chain(task, t -> chainedTask.apply(t).taskOf()).task();
	}
	
	public static Task chain2(Task task, Supplier<Task> chainedTask) {
		return chain2(task.taskOf(), _null -> chainedTask.get());
	}
	
	
	
	public static <T> TaskOf<T> withResult(Task task, Supplier<T> resultWhenComplete) {
		return map(task.taskOf(), _null -> resultWhenComplete.get());
	}
	
	
	
	
	/**
	 * For debugging purposes. Prints a short report on the tasks current state to the
	 * standard output stream
	 * @param title A name for the task, so it can be recognized in the logs. May be null.
	 * @param task The task to report on. Must not be null
	 */
	public static void report(String title, TaskOf<?> task) { 
		report(title == null ? "" : " '" + title + "'", Objects.requireNonNull(task), true, System.out);
	}
	
	/**
	 * For debugging purposes. Prints a short report on the tasks current state to the
	 * standard output stream
	 * @param title A name for the task, so it can be recognized in the logs. May be null.
	 * @param task The task to report on. Must not be null
	 */
	public static void report(String title, Task task) {
		report(title == null ? "" : " '" + title + "'", Objects.requireNonNull(task).taskOf(), false, System.out);
	}
	
	private static void report(String rawTitle, TaskOf<?> task, boolean result, PrintStream stream) {
		stream.println("\nReport on Task" + rawTitle + ":");
		stream.println("Task state: " + task.getState());
		try {
			var r = task.getResult();
			if(result) {
				System.out.println("Task result: " + r + " [" +r.getClass().getName() + "]");
			} else {
				System.out.println("Task completed.");
			}
		} catch (CancellationException | TaskNotDoneException | ExecutionException e) {
			System.out.println("Exception while retrieving result: ");
			e.printStackTrace(stream);
		}
	}
	
	/**
	 * Creates a new task executor that uses the threads provided by the
	 * delegate Executor
	 * @param delegateExecutor An {@link ExecutorService} that manages the threads used for task execution
	 * @return A {@link TaskExecutorService} for that executor
	 */
	public static TaskExecutorService taskExecutor(ExecutorService delegateExecutor) {
		return new WrappingTaskExecutor(delegateExecutor);
	}
	
	/**
	 * Creates a new task executor that uses the threads provided by the
	 * default executor service available through {@link #defaultExecutor()}.
	 * @return A {@link TaskExecutorService} for the default executor
	 */
	public static TaskExecutorService taskExecutor() {
		return taskExecutor(defaultExecutor());
	}
	
	
	private static volatile Supplier<ExecutorService> defaultExecutor = null;
	
	/**
	 * The default {@link ExecutorService} used to execute asynchronous tasks
	 * when no other executor is given. Defaults to {@link ForkJoinPool#commonPool()},
	 * but can be changed through {@link #setDefaultExecutor(Supplier)}.
	 * @return The default executor service
	 */
	public static ExecutorService defaultExecutor() {
		var excsup = defaultExecutor;
		ExecutorService exc;
		if(excsup == null || (exc = excsup.get()) == null) {
			return ForkJoinPool.commonPool();
		} else {
			return exc;
		}
	}
	
	/**
	 * Changes the default {@link ExecutorService} used for task execution. <br>
	 * If {@code serviceSupplier} is {@code null} or returns {@code null} when
	 * called, the {@link ForkJoinPool#commonPool()} will be used as a fallback.
	 * <p>
	 * If a security manager is present, this operation requires a {@link RuntimePermission}
	 * with name {@code "aa4j.setDefaultExecutor"}.
	 * </p>
	 * @param serviceSupplier A supplier producing the executor to use, or {@code null} to reset to default
	 */
	public static void setDefaultExecutor(Supplier<ExecutorService> serviceSupplier) {
		new RuntimePermission("aa4j.setDefaultExecutor").checkGuard(null);
		defaultExecutor = serviceSupplier;
	}
	
	
	private static <T> CompletableFuture<T> newCpf() {
		return new UnobtrudableCompletableFuture<>(); //can change this to use special implementation for everything
	}
	
	private static final class UnobtrudableCompletableFuture<T> extends CompletableFuture<T> {

		@Override
		public CompletableFuture<T> toCompletableFuture() {
			return this;
		}

		@Override
		public void obtrudeValue(T value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void obtrudeException(Throwable ex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> CompletableFuture<U> newIncompleteFuture() {
			return newCpf();
		}
		
	}

}
