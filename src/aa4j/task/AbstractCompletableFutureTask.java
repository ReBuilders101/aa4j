package aa4j.task;

import java.util.Objects;
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

/*package*/ abstract class AbstractCompletableFutureTask<T> implements TaskAccess<T> {
	
	protected final TaskOf<T> taskOf;
	protected final Task task;
	protected final TaskCompletionSource tcs;
	protected final TaskCompletionSourceOf<T> tcsOf;
	protected final CompletableFuture<T> cpf;
	protected final Future<T> future; //Can't use cpf directly
	protected final Awaiter awaiter;
	protected final boolean isCancellableWhileRunning;

	protected AbstractCompletableFutureTask(CompletableFuture<T> usedCpf, boolean canCancelFlag) {
		this.awaiter = new Awaiter();
		this.cpf = usedCpf;
		this.isCancellableWhileRunning = canCancelFlag;
		
		this.taskOf = new TaskOfImpl();
		this.task = new TaskImpl();
		this.tcsOf = new TcsOfImpl();
		this.tcs = new TcsImpl();
		this.future = new FutureImpl();
		
		//The awaiter needs release when the future completes
		cpf.whenComplete((v,e) -> awaiter.signalAll(Awaiter.MASTER_PERMIT));
	}
	
	protected TaskState getStateImpl() {
		if(cpf.isDone()) {
			if(cpf.isCancelled()) return TaskState.CANCELLED;
			if(cpf.isCompletedExceptionally()) return TaskState.FAILED; //cancelled is already excluded
			return TaskState.SUCCEEDED;
		} else {
			return TaskState.RUNNING;
		}
	}
	
	private static Exception extractCause(Exception e) {
		final var c = e.getCause();
		if(c == null) throw new NullPointerException("Exception has no cause");
		return c instanceof Exception ? (Exception) c : new Exception("Wrapping non-Exception Throwable", c);
	}
	
	protected abstract CancelResult cancelImpl();
	
	protected boolean succeedImpl(T value) {
		return cpf.complete(value);
	}
	
	protected boolean failImpl(Exception exception) {
		return cpf.completeExceptionally(validateFailureReason(exception));
	}
	
	private class TaskOfImpl implements TaskOf<T> {

		//The awaits are in Task
		
		@Override
		public TaskOf<T> await() throws InterruptedException {
			task.await();
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly() {
			task.awaitUninterruptibly();
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			task.await(time, unit);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			task.awaitUninterruptibly(time, unit);
			return this;
		}

		@Override
		public TaskOf<T> await(CancellationToken token) throws InterruptedException, CancellationException {
			task.await(token);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(CancellationToken token) throws CancellationException {
			task.awaitUninterruptibly(token);
			return this;
		}

		@Override
		public TaskOf<T> await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			task.await(time, unit, token);
			return this;
		}

		@Override
		public TaskOf<T> awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			task.awaitUninterruptibly(time, unit, token);
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
			return task.sync(time, unit, token, interruptible);
		}

		@Override
		public CancelResult cancel() {
			return cancelImpl();
		}

		@Override
		public TaskState getState() {
			return getStateImpl();
		}

		@Override
		public boolean isCancellable() {
			return isCancellableWhileRunning;
		}

		@Override
		public T getResult() throws ExecutionException, CancellationException, IllegalStateException {
			//Check now, once it is done it will stay that way
			if(!isDone()) throw new IllegalStateException("Task is not done");
			try {
				return cpf.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread had unhadled interrupt when retrieving task result", e);
			}
		}

		@Override
		public T getResultOr(T value) {
			try {
				return cpf.getNow(value);
			} catch (Throwable e) { //This can mean cancellation of a previous stage or error
				return value;
			}
		}

		@Override
		public T getResultOr(T valueWhenIncomplete, T valueWhenFailed, T valueWhenCancelled) {
			try {
				return cpf.getNow(valueWhenIncomplete);
			} catch (CompletionException e) { //This can mean cancellation of a previous stage or error
				final var ex = unwrap(e);
				if(ex instanceof CancellationException) {
					return valueWhenCancelled;
				} else {
					return valueWhenFailed;
				}
			}
		}

		@Override
		public T getResult(Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws CancellationException, IllegalStateException {
			if(!isDone()) throw new IllegalStateException("Task is not done"); //Do this before try/catch
			try {
				return cpf.get();
			} catch (ExecutionException e) { //Fehler muss behandelt werden
				final var ex = extractCause(e);
				throw remainingExs.apply(ex);
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread had unhadled interrupt when retrieving task result", e);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception> T getResult(Class<E1> ex1,
				Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, CancellationException, IllegalStateException {
			if(!isDone()) throw new IllegalStateException("Task is not done"); //Do this before try/catch
			try {
				return cpf.get();
			} catch (ExecutionException e) { //Fehler muss behandelt werden
				final var ex = extractCause(e);
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread had unhadled interrupt when retrieving task result", e);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception, E2 extends Exception> T getResult(Class<E1> ex1, Class<E2> ex2,
				Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, E2, CancellationException, IllegalStateException {
			if(!isDone()) throw new IllegalStateException("Task is not done"); //Do this before try/catch
			try {
				return cpf.get();
			} catch (ExecutionException e) { //Fehler muss behandelt werden
				final var ex = extractCause(e);
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else if(ex2.isInstance(ex)) {
					throw (E2) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread had unhadled interrupt when retrieving task result", e);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E1 extends Exception, E2 extends Exception, E3 extends Exception> T getResult(Class<E1> ex1,
				Class<E2> ex2, Class<E3> ex3, Function<? super Exception, ? extends RuntimeException> remainingExs)
				throws E1, E2, E3, CancellationException, IllegalStateException {
			if(!isDone()) throw new IllegalStateException("Task is not done"); //Do this before try/catch
			try {
				return cpf.get();
			} catch (ExecutionException e) { //Fehler muss behandelt werden
				final var ex = extractCause(e);
				if(ex1.isInstance(ex)) {
					throw (E1) ex;
				} else if(ex2.isInstance(ex)) {
					throw (E2) ex;
				} else if(ex3.isInstance(ex)) {
					throw (E3) ex;
				} else {
					throw remainingExs.apply(ex);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread had unhadled interrupt when retrieving task result", e);
			}
		}

		@Override
		public Future<T> future() {
			return future;
		}

		@Override
		public Task task() {
			return task;
		}

		@Override
		public TaskOf<T> whenDone(Runnable action) {
			cpf.whenComplete(whenDoneImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenSuccess(Consumer<? super T> action) {
			cpf.whenComplete(whenSuccessImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenFailed(Consumer<? super Exception> action) {
			cpf.whenComplete(whenFailedImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenCancelled(Runnable action) {
			cpf.whenComplete(whenCancelledImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenDoneAsync(Runnable action) {
			cpf.whenCompleteAsync(whenDoneImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenSuccessAsync(Consumer<? super T> action) {
			cpf.whenCompleteAsync(whenSuccessImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenFailedAsync(Consumer<? super Exception> action) {
			cpf.whenCompleteAsync(whenFailedImpl(action));
			return this;
		}

		@Override
		public TaskOf<T> whenCancelledAsync(Runnable action) {
			cpf.whenCompleteAsync(whenCancelledImpl(action));
			return this;
		}

		@Override
		public CompletionStage<T> stage() {
			return AbstractCompletableFutureTask.this.stage();
		}
		
	}
	
	private class TaskImpl implements Task {

		@Override
		public Task await() throws InterruptedException {
			awaiter.await(Awaiter.MASTER_PERMIT);
			return this;
		}

		@Override
		public Task awaitUninterruptibly() {
			awaiter.awaitUninterruptibly(Awaiter.MASTER_PERMIT);
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			awaiter.await(Awaiter.MASTER_PERMIT, time, unit); //This checks unit for null
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit) throws TimeoutException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			awaiter.awaitUninterruptibly(Awaiter.MASTER_PERMIT, time, unit); //This checks unit for null
			return this;
		}

		@Override
		public Task await(CancellationToken token) throws InterruptedException, CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
			final var cause = awaiter.await(token);
			if(cause == token) throw new CancellationException("Token was cancelled before task completed");
			return this;
		}

		@Override
		public Task awaitUninterruptibly(CancellationToken token) throws CancellationException {
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(() -> awaiter.signalAll(token), 
						() -> new IllegalArgumentException("Token is already bound to an action"));
			final var cause = awaiter.awaitUninterruptibly(token);
			if(cause == token) throw new CancellationException("Token was cancelled before task completed");
			return this;
		}

		@Override
		public Task await(long time, TimeUnit unit, CancellationToken token)
				throws InterruptedException, TimeoutException, CancellationException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(() -> awaiter.signalAll(token), 
					() -> new IllegalArgumentException("Token is already bound to an action"));
			final var cause = awaiter.await(token, time, unit);
			if(cause == token) throw new CancellationException("Token was cancelled before task completed");
			return this;
		}

		@Override
		public Task awaitUninterruptibly(long time, TimeUnit unit, CancellationToken token)
				throws TimeoutException, CancellationException {
			if(time < 0) throw new IllegalArgumentException("'time' parameter must not be negative");
			Objects.requireNonNull(token, "'token' parameter must not be null")
				.assignAction(() -> awaiter.signalAll(token), 
				() -> new IllegalArgumentException("Token is already bound to an action"));
			final var cause = awaiter.awaitUninterruptibly(token, time, unit);
			if(cause == token) throw new CancellationException("Token was cancelled before task completed");
			return this;
		}

		@Override
		public SyncResult sync(long time, TimeUnit unit, CancellationToken token, boolean interruptible) {
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

		@Override
		public CancelResult cancel() {
			return cancelImpl();
		}
		
		@Override
		public TaskState getState() {
			return getStateImpl();
		}

		@Override
		public boolean isCancellable() {
			return isCancellableWhileRunning;
		}

		@Override
		public Future<?> future() {
			return future;
		}

		@Override
		public TaskOf<?> taskOf() {
			return taskOf;
		}

		@Override
		public Task whenDone(Runnable action) {
			cpf.whenComplete(whenDoneImpl(action));
			return this;
		}

		@Override
		public Task whenSuccess(Runnable action) {
			cpf.whenComplete(whenSuccessImpl(action));
			return this;
		}

		@Override
		public Task whenFailed(Consumer<? super Exception> action) {
			cpf.whenComplete(whenFailedImpl(action));
			return this;
		}

		@Override
		public Task whenCancelled(Runnable action) {
			cpf.whenComplete(whenCancelledImpl(action));
			return this;
		}

		@Override
		public Task whenDoneAsync(Runnable action) {
			cpf.whenCompleteAsync(whenDoneImpl(action));
			return this;
		}

		@Override
		public Task whenSuccessAsync(Runnable action) {
			cpf.whenCompleteAsync(whenSuccessImpl(action));
			return this;
		}

		@Override
		public Task whenFailedAsync(Consumer<? super Exception> action) {
			cpf.whenCompleteAsync(whenFailedImpl(action));
			return this;
		}

		@Override
		public Task whenCancelledAsync(Runnable action) {
			cpf.whenCompleteAsync(whenCancelledImpl(action));
			return this;
		}

		@Override
		public CompletionStage<?> stage() {
			return AbstractCompletableFutureTask.this.stage();
		}

		@Override
		public Task checkSuccess() throws ExecutionException, CancellationException {
			taskOf.getResult(); //This will trigger all exceptions
			return this;
		}

		@Override
		public Task awaitSuccess() throws InterruptedException, ExecutionException, CancellationException {
			taskOf.awaitResult();
			return this;
		}

	}
	
	private class FutureImpl implements Future<T> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return mayInterruptIfRunning ? cancelImpl()  == CancelResult.SUCCESSFULLY_CANCELLED : false;
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
			return taskOf.await().getResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return taskOf.await(timeout, unit).getResult();
		}
		
	}
	
	private class TcsOfImpl implements TaskCompletionSourceOf<T> {

		@Override
		public boolean succeed(T value) {
			return succeedImpl(value);
		}

		@Override
		public boolean fail(Exception exception) {
			return failImpl(exception);
		}

		@Override
		public TaskOf<T> taskOf() {
			return taskOf;
		}
		
	}
	
	private class TcsImpl implements TaskCompletionSource {

		@Override
		public boolean succeed() {
			return succeedImpl(null);
		}

		@Override
		public boolean fail(Exception exception) {
			return failImpl(exception);
		}

		@Override
		public Task task() {
			return task;
		}
		
	}
	
	@Override
	public TaskOf<T> taskOf() {
		return taskOf;
	}

	@Override
	public Task task() {
		return task;
	}

	@Override
	public TaskCompletionSourceOf<T> tcsOf() {
		return tcsOf;
	}

	@Override
	public TaskCompletionSource tcs() {
		return tcs;
	}

	@Override
	public CompletionStage<T> stage() {
		return cpf;
	}

	@Override
	public Future<T> future() {
		return cpf;
	}

	@Override
	public CancellationToken newToken() {
		var t = CancellationToken.unbound();
		t.assignAction(() -> cancelImpl(), ()->null);
		return t;
	}
	
	static <V> BiConsumer<V, ? super Throwable> whenDoneImpl(Runnable action) {
		return (value, ex) -> action.run();
	}
	
	static <V> BiConsumer<V, ? super Throwable> whenFailedImpl(Consumer<? super Exception> action) {
		return (value, th) -> {
			if(th == null) return; //This means success, we don't want that here
			var ex = th instanceof Exception ? (Exception) th : new Exception("Wrapping non-exception Throwable", th);
			
			var exc = unwrap(ex); //The true exception
			if(!(exc instanceof CancellationException)) { //Only if not cancelled
				action.accept(exc);
			}
		};
	}
	
	static <V> BiConsumer<V, ? super Throwable> whenCancelledImpl(Runnable action) {
		return (value, th) -> {
			if(th == null) return; //This means success, we don't want that here
			var ex = th instanceof Exception ? (Exception) th : new Exception("Wrapping non-exception Throwable", th);
			
			var exc = unwrap(ex); //The true exception
			if(exc instanceof CancellationException) { //Only if cancelled
				action.run();
			}
		};
	}
	
	static <V> BiConsumer<V, ? super Throwable> whenSuccessImpl(Consumer<V> action) {
		return (value, ex) -> {
			if(ex != null) return; //Not when an error happened
			
			action.accept(value);
		};
	}
	
	private static <V> BiConsumer<V, ? super Throwable> whenSuccessImpl(Runnable action) {
		return (value, ex) -> {
			if(ex != null) return; //Not when an error happened
			
			action.run();
		};
	}
	
	protected static Throwable validateFailureReason(Throwable ex) {
		if(ex instanceof CompletionException || ex instanceof CancellationException) {
			return new ExecutionException("Using Wrapper exception to fail task", ex);
		} else {
			return ex;
		}
	}
	
	private static Exception unwrap(Exception ex) { //Unwraps all CompletionExceptions 
		if(ex instanceof CompletionException && ex.getCause() != null) {
			return unwrap(extractCause(ex));
		} else {
			return ex;
		}
	}
}
