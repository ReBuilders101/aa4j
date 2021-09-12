package aa4j.task;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import aa4j.AA4JStatic;
import aa4j.TaskNotDoneException;

/*package*/ final class CompletedTask<T> {
	
	static final Task success_untyped = new CompletedTask<>(null, null, TaskState.SUCCEEDED, false).taskView;
	
//	static <T> TaskOf<T> success(T value) {
//		return new TaskOfView<T>(value, null, TaskState.SUCCEEDED, false);
//	}
//	
//	static <T> TaskOf<T> failure(Exception exception) {
//		Objects.requireNonNull(exception, "'exception' parameter cannot be null");
//		return new TaskOfView<T>(null, exception, TaskState.FAILED, false);
//	}
//	
//	static <T> TaskOf<T> cancelled() {
//		return new TaskOfView<T>(null, new CancellationException(), TaskState.CANCELLED, false);
//	}
	
	private ExecutionException throwableFailureReason() {
		return new ExecutionException(unwrapFailureReason());
	}
	private Throwable unwrapFailureReason() {
		return AbstractCompletionStageTask.unwrapFailureReason((Throwable) value);
	}
	private CancellationException unwrapCancellationReason() {
		return (CancellationException) value;
	}
	@SuppressWarnings("unchecked")
	private T unwrapValue() {
		return (T) value;
	}
	
	
	//Similar to AbstractCompletionStageTask, but does not require Awaiter or CompletableFuture.
	private final TaskState state;
	private final Object value; //Can be T, CancellationException, Throwable
	protected final TaskOf<T> taskOfView;
	protected final Task taskView;
	protected final Future<T> futureView;
	private final boolean appearCancellable;
	
	protected CompletedTask(T completionValue, Throwable altValue, TaskState state, boolean cancellable) {
		this.state = state;
		this.appearCancellable = cancellable;
		
		if(state == TaskState.SUCCEEDED) {
			value = completionValue;
		} else if(state == TaskState.CANCELLED) {
			value = new CancellationException();
		} else if(state == TaskState.FAILED) {
			value = AbstractCompletionStageTask.wrapFailureReason(Objects.requireNonNull(altValue));
		} else {
			throw new IllegalArgumentException("Invalid state for CompletedTask: " + state);
		}
		
		this.taskOfView = new TaskOfView();
		this.taskView = new TaskView();
		this.futureView = new FutureView();
	}
	
	
	private final class TaskOfView implements TaskOf<T> {
		
		@Override
		public TaskOf<T> await() throws InterruptedException {
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly() {
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			return this;
		}

		@Override
		public TaskOf<T> await(CancellationToken token) throws InterruptedException, CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(CancellationToken token) throws CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
			if(token != null) token.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return SyncResult.THREAD_SYNCHRONIZED;
		}

		@Override
		public CancelResult cancel() {
			//only done states are possible
			if(state.isSuccess()) return CancelResult.ALREADY_SUCCEEDED;
			if(state.isFailed()) return CancelResult.ALREADY_FAILED;
			return CancelResult.ALREADY_CANCELLED;
		}

		@Override
		public TaskState getState() {
			return state;
		}

		@Override
		public boolean isCancellable() {
			return appearCancellable;
		}

		@Override
		public T getResult() throws ExecutionException, CancellationException, TaskNotDoneException {
			if(state.isCancelled()) {
				throw unwrapCancellationReason();
			} else if(state.isFailed()) {
				throw throwableFailureReason();
			} else {
				return unwrapValue();
			}
		}

		@Override
		public T getResultOr(T altValue) {
			if(state.isSuccess()) {
				return unwrapValue();
			} else {
				return altValue;
			}
		}

		@Override
		public T getResultOr(T valueWhenIncomplete, T valueWhenFailed, T valueWhenCancelled) {
			if(state.isSuccess()) {
				return unwrapValue();
			} else if(state.isFailed()) {
				return valueWhenFailed;
			} else { //No other state possible
				return valueWhenCancelled;
			}
		}

		@Override
		public T getResult(Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws CancellationException, TaskNotDoneException {
			if(state.isSuccess()) {
				return unwrapValue();
			} else if(state.isFailed()) {
				throw remainingExs.apply(unwrapFailureReason());
			} else { //Cancelled
				throw unwrapCancellationReason();
			}
		}

		
		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable> T getResult(Class<E1> ex1,
				Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, CancellationException, TaskNotDoneException {
			if(state.isSuccess()) {
				return unwrapValue();
			} else if(state.isFailed()) {
				//value is some throwable
				final var ex = unwrapFailureReason();
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} else { //Cancelled
				throw unwrapCancellationReason();
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable, E2 extends Throwable> T getResult(Class<E1> ex1, Class<E2> ex2,
				Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, E2, CancellationException, TaskNotDoneException {
			if(state.isSuccess()) {
				return unwrapValue();
			} else if(state.isFailed()) {
				final var ex = unwrapFailureReason();
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else if(ex2.isInstance(ex)) {
					throw (E2) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} else {
				throw unwrapCancellationReason();
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> T getResult(Class<E1> ex1,
				Class<E2> ex2, Class<E3> ex3, Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, E2, E3, CancellationException, TaskNotDoneException {
			if(state.isSuccess()) {
				return unwrapValue();
			} else if(state.isFailed()) {
				final var ex = unwrapFailureReason();
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else if(ex2.isInstance(ex)) {
					throw (E2) ex;
				} else if(ex3.isInstance(ex)) {
					throw (E3) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} else {
				throw unwrapCancellationReason();
			}
		}
		
		@Override
		public Optional<T> getResultIfSuccess() {
			if(state.isSuccess()) {
				return Optional.ofNullable(unwrapValue());
			} else {
				return Optional.empty();
			}
		}

		@Override
		public Optional<T> getResultIfPresent() throws ExecutionException, CancellationException {
			if(state.isSuccess()) {
				return Optional.ofNullable(unwrapValue());
			} else if(state.isCancelled()) {
				throw unwrapCancellationReason();
			} else if(state.isFailed()) {
				throw throwableFailureReason(); 
			} else {
				return Optional.empty();
			}
		}
		

		@Override
		public Future<T> future() {
			return futureView;
		}

		@Override
		@SuppressWarnings("unchecked")
		public CompletionStage<T> stage() {
			//Directly copy, without unwrap calls
			if(state.isSuccess()) {
				return CompletableFuture.completedStage((T) value);
			} else {
				return CompletableFuture.failedStage((Throwable) value);
			}
		}

		@Override
		public Task task() {
			return taskView;
		}

		@Override
		public TaskOf<T> whenDone(Runnable action) {
			action.run();
			return this;
		}

		@Override
		public TaskOf<T> whenSuccess(Consumer<? super T> action) {
			if(state.isSuccess()) {
				action.accept(unwrapValue());
			}
			return this;
		}

		@Override
		public TaskOf<T> whenFailed(Consumer<? super Throwable> action) {
			if(state.isFailed()) {
				action.accept(unwrapFailureReason());
			}
			return this;
		}

		@Override
		public TaskOf<T> whenCancelled(Runnable action) {
			if(state.isCancelled()) {
				action.run();
			}
			return this;
		}

		@Override
		public TaskOf<T> whenDoneAsync(Runnable action) {
			//use common pool
			Tasks.defaultExecutor().submit(action);
			return this;
		}
		
		@Override
		public TaskOf<T> whenSuccessAsync(Consumer<? super T> action) {
			if(state.isSuccess()) {
				Tasks.defaultExecutor().submit(() -> action.accept(unwrapValue()));
			}
			return this;
		}

		@Override
		public TaskOf<T> whenFailedAsync(Consumer<? super Throwable> action) {
			if(state.isFailed()) {
				Tasks.defaultExecutor().submit(() -> action.accept(unwrapFailureReason()));
			}
			return this;
		}

		@Override
		public TaskOf<T> whenCancelledAsync(Runnable action) {
			if(state.isCancelled()) {
				Tasks.defaultExecutor().submit(action);
			}
			return this;
		}
	}
	
	
	private final class TaskView implements Task {
		
		@Override
		public Task await() throws InterruptedException {
			return this;
		}

		@Override
		public Task awaitUninterruptibly() {
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			return this;
		}

		@Override
		public Task await(CancellationToken token) throws InterruptedException, CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public Task awaitUninterruptibly(CancellationToken token) throws CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			Objects.requireNonNull(unit, "'unit' parameter must not be null");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
			if(token != null) token.assignAction(AA4JStatic.NOOP, () -> new IllegalArgumentException("Token is already bound to an action"));
			return SyncResult.THREAD_SYNCHRONIZED;
		}

		@Override
		public CancelResult cancel() {
			if(state.isSuccess()) return CancelResult.ALREADY_SUCCEEDED;
			if(state.isFailed()) return CancelResult.ALREADY_FAILED;
			return CancelResult.ALREADY_CANCELLED;
		}

		@Override
		public Task checkSuccess() throws ExecutionException, CancellationException {
			if(state.isCancelled()) throw unwrapCancellationReason();
			if(state.isFailed()) throw throwableFailureReason();
			return this;
		}

		@Override
		public Task awaitSuccess() throws InterruptedException, ExecutionException, CancellationException {
			return await().checkSuccess();
		}

		@Override
		public TaskState getState() {
			return state;
		}

		@Override
		public boolean isCancellable() {
			return appearCancellable;
		}

		@Override
		public Future<?> future() {
			return futureView;
		}

		@Override
		public CompletionStage<?> stage() {
			//delegate this, b/c taskOfView knows about T and canc infer that return type
			return taskOfView.stage();
		}

		@Override
		public TaskOf<?> taskOf() {
			return taskOfView;
		}

		@Override
		public Task whenDone(Runnable action) {
			action.run();
			return this;
		}

		@Override
		public Task whenSuccess(Runnable action) {
			if(state.isSuccess()) action.run();
			return this;
		}

		@Override
		public Task whenFailed(Consumer<? super Throwable> action) {
			if(state.isFailed()) action.accept(unwrapFailureReason());
			return this;
		}

		@Override
		public Task whenCancelled(Runnable action) {
			if(state.isCancelled()) action.run();
			return this;
		}

		@Override
		public Task whenDoneAsync(Runnable action) {
			Tasks.defaultExecutor().submit(action);
			return this;
		}

		@Override
		public Task whenSuccessAsync(Runnable action) {
			if(state.isSuccess()) Tasks.defaultExecutor().submit(action);
			return this;
		}

		@Override
		public Task whenFailedAsync(Consumer<? super Throwable> action) {
			if(state.isFailed()) Tasks.defaultExecutor().submit(() -> action.accept(unwrapFailureReason()));
			return this;
		}

		@Override
		public Task whenCancelledAsync(Runnable action) {
			if(state.isCancelled()) Tasks.defaultExecutor().submit(action);
			return this;
		}
	}
	
	private final class FutureView implements Future<T> {
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return state.isCancelled();
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return taskOfView.getResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			Objects.requireNonNull(unit, "'unit' parameter must not be null"); //can help to catch cases where this is null acccidentally
			//done, so no wait anyways
			return taskOfView.getResult();
		}
		
	}

}
