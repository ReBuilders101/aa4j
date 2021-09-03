package aa4j.task;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import aa4j.AA4JStatic;

/*package*/ final class CompletedTasks {
	private CompletedTasks() { throw new RuntimeException("No instance for you"); }
	
	static final Task success_untyped = success(null).task();
	
	static <T> TaskOf<T> success(T value) {
		return new AnyResult<T>(value, null, TaskState.SUCCEEDED, false);
	}
	
	static <T> TaskOf<T> failure(Exception exception) {
		return new AnyResult<T>(null, exception, TaskState.FAILED, false);
	}
	
	static <T> TaskOf<T> cancelled() {
		return new AnyResult<T>(null, new CancellationException(), TaskState.CANCELLED, false);
	}
	
	private static final class AnyResult<T> implements TaskOf<T> {

		private final TaskState state;
		private final Object value; //one of T, CancellationException, Throwable
		private final CompletableFuture<T> doneCpf;
		private final NoResult view;
		private final boolean appearCancellable;
		private final FutureView<T> view2;
		
		//We cant do constrcutors with T, CancellationException, Throwable b/c T might be one of those
		private AnyResult(T cmpValue, Exception altValue, TaskState desiredState, boolean appearCancellable) {
			if(desiredState == TaskState.SUCCEEDED) {
				this.value = cmpValue;
				this.doneCpf = CompletableFuture.completedFuture(cmpValue);
			} else if(desiredState == TaskState.CANCELLED) {
				Objects.requireNonNull(altValue, "'altValue' parameter must not be null");
				if(altValue instanceof CancellationException) {
					this.value = altValue;
					this.doneCpf = CompletableFuture.failedFuture(altValue);
				} else {
					throw new IllegalArgumentException("Cannot use state CANCELLED without CancellationException");
				}
			} else if(desiredState == TaskState.FAILED) {
				Objects.requireNonNull(altValue, "'altValue' parameter must not be null");
				final Exception av2;
				if(altValue instanceof CancellationException) {
					//We need to wrap
					av2 = new ExecutionException("Need to wrap CancellationException to fail future", altValue);
				} else {
					av2 = altValue;
				}
				this.value = av2;
				this.doneCpf = CompletableFuture.failedFuture(av2);
			} else {
				throw new IllegalArgumentException("Cannot use state " + desiredState + " for completed stage");
			}
			
			this.state = desiredState;
			this.view = new NoResult(this);
			this.view2 = new FutureView<>(this);
			this.appearCancellable = appearCancellable;
		}
		
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
		@SuppressWarnings("unchecked")
		public T getResult() throws ExecutionException, CancellationException, IllegalStateException {
			if(state.isCancelled()) {
				throw (CancellationException) value;
			} else if(state.isFailed()) {
				throw new ExecutionException((Exception) value);
			} else {
				return (T) value;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getResultOr(T altValue) {
			if(state.isSuccess()) {
				return (T) value;
			} else {
				return altValue;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getResultOr(T valueWhenIncomplete, T valueWhenFailed, T valueWhenCancelled) {
			if(state.isSuccess()) {
				return (T) value;
			} else if(state.isFailed()) {
				return valueWhenFailed;
			} else { //No other state possible
				return valueWhenCancelled;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getResult(Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws CancellationException, IllegalStateException {
			if(state.isSuccess()) {
				return (T) value;
			} else if(state.isFailed()) {
				throw remainingExs.apply((Exception) value);
			} else { //Cancelled
				throw (CancellationException) value;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception> T getResult(Class<E1> ex1,
				Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, CancellationException, IllegalStateException {
			if(state.isSuccess()) {
				return (T) value;
			} else if(state.isFailed()) {
				//value is some throwable
				if(ex1.isInstance(value)) {
					throw (E1) value;
				} else {
					throw remainingExs.apply((Exception) value);
				}
			} else { //Cancelled
				throw (CancellationException) value;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception, E2 extends Exception> T getResult(Class<E1> ex1, Class<E2> ex2,
				Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, E2, CancellationException, IllegalStateException {
			if(state.isSuccess()) {
				return (T) value;
			} else if(state.isFailed()) {
				//value is some throwable
				if(ex1.isInstance(value)) {
					throw (E1) value;
				} else if(ex2.isInstance(value)) {
					throw (E2) value;
				} else {
					throw remainingExs.apply((Exception) value);
				}
			} else { //Cancelled
				throw (CancellationException) value;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception, E2 extends Exception, E3 extends Exception> T getResult(Class<E1> ex1,
				Class<E2> ex2, Class<E3> ex3, Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, E2, E3, CancellationException, IllegalStateException {
			if(state.isSuccess()) {
				return (T) value;
			} else if(state.isFailed()) {
				//value is some throwable
				if(ex1.isInstance(value)) {
					throw (E1) value;
				} else if(ex2.isInstance(value)) {
					throw (E2) value;
				} else if(ex3.isInstance(value)) {
					throw (E3) value;
				} else {
					throw remainingExs.apply((Exception) value);
				}
			} else { //Cancelled
				throw (CancellationException) value;
			}
		}

		@Override
		public Future<T> future() {
			return view2;
		}

		@Override
		public CompletionStage<T> stage() {
			return doneCpf;
		}

		@Override
		public Task task() {
			return view;
		}

		@Override
		public TaskOf<T> whenDone(Runnable action) {
			action.run();
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public TaskOf<T> whenSuccess(Consumer<? super T> action) {
			if(state.isSuccess()) {
				action.accept((T) value);
			}
			return this;
		}

		@Override
		public TaskOf<T> whenFailed(Consumer<? super Exception> action) {
			if(state.isFailed()) {
				action.accept((Exception) value);
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
			ForkJoinPool.commonPool().submit(action);
			return this;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public TaskOf<T> whenSuccessAsync(Consumer<? super T> action) {
			if(state.isSuccess()) {
				ForkJoinPool.commonPool().submit(() -> action.accept((T) value));
			}
			return this;
		}

		@Override
		public TaskOf<T> whenFailedAsync(Consumer<? super Exception> action) {
			if(state.isFailed()) {
				ForkJoinPool.commonPool().submit(() -> action.accept((Exception) value));
			}
			return this;
		}

		@Override
		public TaskOf<T> whenCancelledAsync(Runnable action) {
			if(state.isCancelled()) {
				ForkJoinPool.commonPool().submit(action);
			}
			return this;
		}
		
	}
	
	private static final class NoResult implements Task {

		private final AnyResult<?> taskOf;
		
		private NoResult(AnyResult<?> taskOf) {
			this.taskOf = taskOf;
		}
		
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
			 return taskOf.cancel();
		}

		@Override
		public Task checkSuccess() throws ExecutionException, CancellationException {
			taskOf.getResult();
			return this;
		}

		@Override
		public Task awaitSuccess() throws InterruptedException, ExecutionException, CancellationException {
			taskOf.awaitResult();
			return this;
		}

		@Override
		public TaskState getState() {
			return taskOf.getState();
		}

		@Override
		public boolean isCancellable() {
			return taskOf.isCancellable();
		}

		@Override
		public Future<?> future() {
			return taskOf.future();
		}

		@Override
		public CompletionStage<?> stage() {
			return taskOf.stage();
		}

		@Override
		public TaskOf<?> taskOf() {
			return taskOf;
		}

		@Override
		public Task whenDone(Runnable action) {
			taskOf.whenDone(action);
			return this;
		}

		@Override
		public Task whenSuccess(Runnable action) {
			taskOf.whenSuccess((a) -> action.run());
			return this;
		}

		@Override
		public Task whenFailed(Consumer<? super Exception> action) {
			taskOf.whenFailed(action);
			return this;
		}

		@Override
		public Task whenCancelled(Runnable action) {
			taskOf.whenCancelled(action);
			return this;
		}

		@Override
		public Task whenDoneAsync(Runnable action) {
			taskOf.whenDoneAsync(action);
			return this;
		}

		@Override
		public Task whenSuccessAsync(Runnable action) {
			taskOf.whenSuccessAsync((a) -> action.run());
			return this;
		}

		@Override
		public Task whenFailedAsync(Consumer<? super Exception> action) {
			taskOf.whenFailedAsync(action);
			return this;
		}

		@Override
		public Task whenCancelledAsync(Runnable action) {
			taskOf.whenCancelledAsync(action);
			return this;
		}
		
		
	}
	
	private static final class FutureView<T> implements Future<T> {

		private final AnyResult<T> taskOf;
		
		private FutureView(AnyResult<T> taskOf) {
			this.taskOf = taskOf;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return taskOf.isCancelled();
		}

		@Override
		public boolean isDone() {
			return taskOf.isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return taskOf.getResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			Objects.requireNonNull(unit, "'unit' parameter must not be null"); //can help to catch cases where this is null acccidentally
			return taskOf.getResult();
		}
		
	}

}
