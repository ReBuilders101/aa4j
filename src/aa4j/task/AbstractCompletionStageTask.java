package aa4j.task;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import aa4j.Awaiter;
import aa4j.TaskNotDoneException;

/**
 * Provides methods necessary for a {@link Task} and {@link TaskOf} that are backed by a completableFuture
 * @param <T> Task result type
 */
/*package*/ abstract class AbstractCompletionStageTask<T> {

	protected final Awaiter awaiter;
	protected final Task taskView;
	protected final TaskOf<T> taskOfView;
	protected final Future<T> futureView;
	protected final CompletableFuture<T> stage;
	
	protected AbstractCompletionStageTask(CompletionStage<T> future) {
		this.awaiter = new Awaiter();
		this.stage = future.toCompletableFuture();
		
		this.taskView = new TaskImpl();
		this.taskOfView = new TaskOfImpl();
		this.futureView = new FutureImpl();
	}
	
	protected TaskState getStateImpl() {
		if(stage.isDone()) {
			if(stage.isCancelled()) return TaskState.CANCELLED;
			if(stage.isCompletedExceptionally()) return TaskState.FAILED; //cancelled is already excluded
			return TaskState.SUCCEEDED;
		} else {
			return TaskState.RUNNING;
		}
	}
	
	protected abstract CancelResult cancelImpl();
	
	protected void awaitImpl() throws InterruptedException {
		awaiter.await(Awaiter.MASTER_PERMIT);
	}
	
	protected void awaitUninterruptiblyImpl() {
		awaiter.awaitUninterruptibly(Awaiter.MASTER_PERMIT);
	}
	
	protected void awaitImpl(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
		awaiter.await(Awaiter.MASTER_PERMIT, time, unit);
	}
	
	protected void awaitUninterruptiblyImpl(long time, TimeUnit unit) throws TimeoutException {
		if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
		awaiter.awaitUninterruptibly(Awaiter.MASTER_PERMIT, time, unit);
	}
	
	protected void awaitImpl(CancellationToken token) throws InterruptedException, CancellationException {
		Objects.requireNonNull(token, "'token' parameter must not be null")
		.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
		final var cause = awaiter.await(token);
		if(cause == token) throw new CancellationException("Token was cancelled before task completed");
	}
	
	protected void awaitUninterruptiblyImpl(CancellationToken token) throws CancellationException {
		Objects.requireNonNull(token, "'token' parameter must not be null")
		.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
		final var cause = awaiter.awaitUninterruptibly(token);
		if(cause == token) throw new CancellationException("Token was cancelled before task completed");
	}
	
	protected void awaitImpl(long time, TimeUnit unit, CancellationToken token)
			throws InterruptedException, TimeoutException, CancellationException {
		if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
		Objects.requireNonNull(token, "'token' parameter must not be null")
		.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
		final var cause = awaiter.await(token, time, unit);
		if(cause == token) throw new CancellationException("Token was cancelled before task completed");
	}
	
	protected void awaitUninterruptiblyImpl(long time, TimeUnit unit, CancellationToken token)
			throws TimeoutException, CancellationException {
		if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
		Objects.requireNonNull(token, "'token' parameter must not be null")
		.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
		final var cause = awaiter.awaitUninterruptibly(token, time, unit);
		if(cause == token) throw new CancellationException("Token was cancelled before task completed");
	}
	
	protected SyncResult syncImpl(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
		//Choose what to do
		final boolean useTimeout = (unit != null && time >= 0);
		final boolean useToken = (token != null);
		
		//Token stuff
		final Object permit;
		if(useToken) {
			token.assignAction(() -> awaiter.signalAll(token), 
					() -> new IllegalArgumentException("Token is already bound to an action"));
			permit = token;
		} else {
			permit = Awaiter.MASTER_PERMIT;
		}
		
		//The call
		final Object cause;
		try {
			if(interruptible && useTimeout) {
				cause = awaiter.await(permit, time, unit);
			} else if(!interruptible && useTimeout) {
				cause = awaiter.awaitUninterruptibly(permit, time, unit);
			} else if(interruptible && !useTimeout) {
				cause = awaiter.await(permit);
			} else { /*!interruptible && !useTimeout*/
				cause = awaiter.awaitUninterruptibly(permit);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return SyncResult.THREAD_INTERRUPTED;
		} catch (TimeoutException e) {
			return SyncResult.WAIT_TIMEOUT;
		}
		
		if(useToken && cause == permit) {
			return SyncResult.WAIT_CANCELLED;
		}
		
		return SyncResult.THREAD_SYNCHRONIZED;
	}
	
	private final class TaskImpl implements Task {

		@Override
		public Task await() throws InterruptedException {
			awaitImpl();
			return this;
		}

		@Override
		public Task awaitUninterruptibly() {
			awaitUninterruptiblyImpl();
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			awaitImpl(time, unit);
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			awaitUninterruptiblyImpl(time, unit);
			return this;
		}

		@Override
		public Task await(CancellationToken token) throws InterruptedException, CancellationException {
			awaitImpl(token);
			return this;
		}

		@Override
		public Task awaitUninterruptibly(CancellationToken token) throws CancellationException {
			awaitUninterruptiblyImpl(token);
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			awaitImpl(time, unit, token);
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			awaitUninterruptiblyImpl(time, unit, token);
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
			return syncImpl(time, unit, token, interruptible);
		}

		@Override
		public CancelResult cancel() {
			return cancelImpl();
		}

		@Override
		public Task checkSuccess() throws ExecutionException, CancellationException {
			taskOfView.getResult(); //Just ignore the result
			return this;
		}

		@Override
		public Task awaitSuccess() throws InterruptedException, ExecutionException, CancellationException {
			return await().checkSuccess();
		}

		@Override
		public TaskState getState() {
			return getStateImpl();
		}

		@Override
		public Future<?> future() {
			return futureView;
		}

		@Override
		public CompletionStage<?> stage() {
			return stage;
		}

		@Override
		public TaskOf<?> taskOf() {
			return taskOfView;
		}

		@Override
		public Task whenDone(Runnable action) {
			stage.whenComplete(whenDoneImpl(action));
			return this;
		}

		@Override
		public Task whenSuccess(Runnable action) {
			stage.whenComplete(whenSuccessImpl(action));
			return this;
		}

		@Override
		public Task whenFailed(Consumer<? super Throwable> action) {
			stage.whenComplete(whenFailedImpl(action));
			return this;
		}

		@Override
		public Task whenCancelled(Runnable action) {
			stage.whenComplete(whenCancelledImpl(action));
			return this;
		}

		@Override
		public Task whenDoneAsync(Runnable action) {
			stage.whenCompleteAsync(whenDoneImpl(action));
			return this;
		}

		@Override
		public Task whenSuccessAsync(Runnable action) {
			stage.whenCompleteAsync(whenSuccessImpl(action));
			return this;
		}

		@Override
		public Task whenFailedAsync(Consumer<? super Throwable> action) {
			stage.whenCompleteAsync(whenFailedImpl(action));
			return this;
		}

		@Override
		public Task whenCancelledAsync(Runnable action) {
			stage.whenCompleteAsync(whenCancelledImpl(action));
			return this;
		}
		
	}
	
	private final class TaskOfImpl implements TaskOf<T> {

		@Override
		public TaskOf<T> await() throws InterruptedException {
			awaitImpl();
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly() {
			awaitUninterruptiblyImpl();
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			awaitImpl(time, unit);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			awaitUninterruptiblyImpl(time, unit);
			return this;
		}

		@Override
		public TaskOf<T> await(CancellationToken token) throws InterruptedException, CancellationException {
			awaitImpl(token);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(CancellationToken token) throws CancellationException {
			awaitUninterruptiblyImpl(token);
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			awaitImpl(time, unit, token);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			awaitUninterruptiblyImpl(time, unit, token);
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
			return syncImpl(time, unit, token, interruptible);
		}

		@Override
		public CancelResult cancel() {
			return cancelImpl();
		}

		@Override
		public TaskState getState() {
			return getStateImpl();
		}

		private T getResultIfComplete() throws CompletionException, CancellationException, TaskNotDoneException {
			final Object sentinel = new Object(); //unique object only available in this scope
			
			//The one time I am thankful that java generics cast everything to Object anyways
			@SuppressWarnings("unchecked")
			final var res = stage.getNow((T) sentinel);
			
			if(res == sentinel) {
				throw new TaskNotDoneException();
			} else {
				return res;
			}
		}
		
		@Override
		public T getResult() throws ExecutionException, CancellationException, TaskNotDoneException {
			try {
				return getResultIfComplete();
			} catch (CompletionException ex) {
				//getNow gives a CompletionException, but we want an ExecutionException.
				throw unwrapThrownFailureReason(ex);
			} //the CancellationException can pass right through
			
		}

		@Override
		public T getResultOr(T value) {
			try {
				return stage.getNow(value);
			} catch (CompletionException | CancellationException ex) {
				return value;
			}
		}

		@Override
		public T getResultOr(T valueWhenIncomplete, T valueWhenFailed, T valueWhenCancelled) {
			try {
				return stage.getNow(valueWhenIncomplete);
			} catch (CancellationException ex) {
				return valueWhenCancelled;
			} catch (CompletionException ex) {
				return valueWhenFailed;
			}
		}

		@Override
		public T getResult(Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws CancellationException, TaskNotDoneException {
			try {
				return getResultIfComplete();
			} catch (CompletionException e) {
				final var innerEx = unwrapFailureReason(e);
				throw remainingExs.apply(innerEx);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable> T getResult(Class<E1> ex1,
				Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, CancellationException, TaskNotDoneException {
			try {
				return getResultIfComplete();
			} catch (CompletionException e) {
				final var innerEx = unwrapFailureReason(e);
				
				if(ex1.isInstance(innerEx)) {
					throw (E1) innerEx;
				} else {
					throw remainingExs.apply(innerEx);
				}
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable, E2 extends Throwable> T getResult(Class<E1> ex1, Class<E2> ex2,
				Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, E2, CancellationException, TaskNotDoneException {
			try {
				return getResultIfComplete();
			} catch (CompletionException e) {
				final var innerEx = unwrapFailureReason(e);
				
				if(ex1.isInstance(innerEx)) {
					throw (E1) innerEx;
				} else if(ex2.isInstance(innerEx)) {
					throw (E2) innerEx;
				} else {
					throw remainingExs.apply(innerEx);
				}
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> T getResult(Class<E1> ex1,
				Class<E2> ex2, Class<E3> ex3, Function<? super Throwable, ? extends RuntimeException> remainingExs)
				throws E1, E2, E3, CancellationException, TaskNotDoneException {
			try {
				return getResultIfComplete();
			} catch (CompletionException e) {
				final var innerEx = unwrapFailureReason(e);
				
				if(ex1.isInstance(innerEx)) {
					throw (E1) innerEx;
				} else if(ex2.isInstance(innerEx)) {
					throw (E2) innerEx;
				} else if(ex3.isInstance(innerEx)) {
					throw (E3) innerEx;
				} else {
					throw remainingExs.apply(innerEx);
				}
			}
		}
		
		@Override
		public Optional<T> getResultIfSuccess() {
			try {
				return Optional.ofNullable(getResultIfComplete());
			} catch (CompletionException | CancellationException | TaskNotDoneException e) {
				return Optional.empty();
			}
		}

		@Override
		public Optional<T> getResultIfPresent() throws ExecutionException, CancellationException {
			try {
				return Optional.ofNullable(getResult());
			} catch (TaskNotDoneException e) {
				return Optional.empty();
			}
		}

		@Override
		public Future<T> future() {
			return futureView;
		}

		@Override
		public CompletionStage<T> stage() {
			return stage;
		}

		@Override
		public Task task() {
			return taskView;
		}

		@Override
		public TaskOf<T> whenDone(Runnable action) {
			stage.whenComplete(whenDoneImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenSuccess(Consumer<? super T> action) {
			stage.whenComplete(whenSuccessImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenFailed(Consumer<? super Throwable> action) {
			stage.whenComplete(whenFailedImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenCancelled(Runnable action) {
			stage.whenComplete(whenCancelledImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenDoneAsync(Runnable action) {
			stage.whenCompleteAsync(whenDoneImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenSuccessAsync(Consumer<? super T> action) {
			stage.whenCompleteAsync(whenSuccessImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenFailedAsync(Consumer<? super Throwable> action) {
			stage.whenCompleteAsync(whenFailedImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenCancelledAsync(Runnable action) {
			stage.whenCompleteAsync(whenCancelledImpl(action));
			return this;
		}
		
	}
	
	private final class FutureImpl implements Future<T> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return mayInterruptIfRunning ? cancelImpl().isCancelledByAction() : false;
		}

		@Override
		public boolean isCancelled() {
			return getStateImpl().isCancelled();
		}

		@Override
		public boolean isDone() {
			return getStateImpl().isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return taskOfView.awaitResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return taskOfView.await(timeout, unit).getResult();
		}
		
	}

	/**
	 * Contains an exception that otherwise would have a special meaning inside the
	 * CompletableFuture. Those are: CancellationException, CompletionException
	 */
	private static final class LiteralException extends Exception {
		private static final long serialVersionUID = -2172236355312586767L;
		
		public LiteralException(Throwable cause) {
			super(Objects.requireNonNull(cause));
		}
	}
	

	protected CancelResult fromDoneState() {
		var state = getStateImpl();
		if(!state.isDone()) throw new IllegalStateException("State is not done");
		if(state.isSuccess()) return CancelResult.ALREADY_SUCCEEDED;
		if(state.isFailed()) return CancelResult.ALREADY_FAILED;
		return CancelResult.ALREADY_CANCELLED;
	}
	
	protected static <V> BiConsumer<V, ? super Throwable> whenDoneImpl(Runnable action) {
		Objects.requireNonNull(action);
		return (value, ex) -> action.run();
	}
	
	protected static <V> BiConsumer<V, ? super Throwable> whenFailedImpl(Consumer<? super Throwable> action) {
		Objects.requireNonNull(action);
		return (value, ex) -> {
			if(ex != null && !isCancelledBy(ex)) {
				action.accept(unwrapFailureReason(ex));
			}
		};
	}
	
	protected static <V> BiConsumer<V, ? super Throwable> whenCancelledImpl(Runnable action) {
		Objects.requireNonNull(action);
		return (value, ex) -> {
			if(ex != null && isCancelledBy(ex)) {
				action.run();
			}
		};
	}
	
	protected static <V> BiConsumer<V, ? super Throwable> whenSuccessImpl(Consumer<V> action) {
		Objects.requireNonNull(action);
		return (value, ex) -> {
			if(ex == null)  {
				action.accept(value);
			}
		};
	}
	
	protected static <V> BiConsumer<V, ? super Throwable> whenSuccessImpl(Runnable action) {
		Objects.requireNonNull(action);
		return (value, ex) -> {
			if(ex == null)  {
				action.run();
			}
		};
	}
	
	/**
	 * Checks whether an exception as a CompletableFutures result
	 * result means cancellation
	 * @param ex not null
	 * @return
	 */
	private static boolean isCancelledBy(Throwable ex) {
		if(ex == null) return false;
		return unwrapHandler(ex) instanceof CancellationException;
	}
	
	/**
	 * Extracts form {@link CompletionException}s 
	 * @param ex the exception
	 * @return ex, or the extracted cause
	 */
	private static Throwable unwrapHandler(Throwable ex) {
		if(ex == null) return null;
		
		if(ex instanceof CompletionException) {
			return ex.getCause();
		} else {
			return ex;
		}
	}
	
	
	
	/**
	 * If we call fail(CancellationException), this would cancel the task instead.
	 * Instead we wrap it in a LiteralException and unwrap later
	 * @param ex the throwable to (possibly) wrap
	 * @return a wrapped exception 
	 */
	protected static Throwable wrapFailureReason(Throwable ex) {
		if(ex == null) return null;
		
		if(ex instanceof CancellationException || ex instanceof CompletionException || ex instanceof ExecutionException) {
			return new LiteralException(ex);
		} else {
			return ex;
		}
	}
	
	protected static Throwable unwrapFailureReason(Throwable ex) {
		if(ex == null) return null;
		
		final var realEx = ex instanceof CompletionException ? ex.getCause() : ex;
		return realEx instanceof LiteralException ? realEx.getCause() : realEx;
	}
	
	@Deprecated //unused
	protected static ExecutionException unwrapThrownFailureReason(ExecutionException ex) throws CancellationException {
		var cause = Objects.requireNonNull(ex).getCause();
		
		if(cause instanceof LiteralException) {
			return new ExecutionException(ex.getMessage(), cause.getCause());
		} else if(cause instanceof CancellationException) {
			throw (CancellationException) cause;
		} else {
			return ex;
		}
	}
	
	protected static ExecutionException unwrapThrownFailureReason(CompletionException ex) throws CancellationException{
		var cause = Objects.requireNonNull(ex).getCause();
		
		if(cause instanceof LiteralException) {
			return new ExecutionException(ex.getMessage(), cause.getCause());
		} else if(cause instanceof CancellationException) {
			throw (CancellationException) cause;
		} else {
			return new ExecutionException(ex.getMessage(), ex.getCause());
		}
	}
}
