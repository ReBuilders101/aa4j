package aa4j.task;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * A task that represents waiting for some action to occur that completes the task.
 * @param <T> The result type of the task
 */
/*package*/ final class NonBlockingTask<T> extends AbstractCompletionStageTask<T> implements TaskAccess<T> {
	
	private final TaskCompletionSource tcsView;
	private final TaskCompletionSourceOf<T> tcsOfView;
	private final boolean isCancellable;
	
	protected NonBlockingTask(CompletableFuture<T> usedCpf, Runnable cancellationHandler) {
		this(usedCpf, true);
		Objects.requireNonNull(cancellationHandler);
		stage.whenComplete(whenCancelledImpl(cancellationHandler));
	}
	
	protected NonBlockingTask(CompletableFuture<T> usedCpf, boolean canCancel) {
		super(usedCpf);
		this.isCancellable = canCancel;
		this.tcsView = new TCS();
		this.tcsOfView = new TCSOf();
	}

	@Override
	protected CancelResult cancelImpl() {
		if(isCancellable) { //The easy case. We can cancel whenever
			if(stage.completeExceptionally(new CancellationException())) {
				return CancelResult.SUCCESSFULLY_CANCELLED;
			} else { //It better be done then
				return fromDoneState();
			}
		} else { //cannot even try to cancel, either unable or already 
			return getStateImpl().isDone() ? fromDoneState() : CancelResult.UNABLE_TO_CANCEL;
		}
	}

	protected boolean succeedImpl(T value) {
		return stage.complete(value);
	}
	
	protected boolean failImpl(Throwable ex) {
		return stage.completeExceptionally(wrapFailureReason(ex));
	}
	
	@Override
	public TaskOf<T> taskOf() {
		return taskOfView;
	}

	@Override
	public Task task() {
		return taskView;
	}

	@Override
	public TaskCompletionSourceOf<T> tcsOf() {
		return tcsOfView;
	}

	@Override
	public TaskCompletionSource tcs() {
		return tcsView;
	}

	@Override
	public CompletionStage<T> stage() {
		return stage;
	}

	@Override
	public Future<T> future() {
		return futureView;
	}

	@Override
	public CancellationToken newToken() {
		var t = CancellationToken.unbound();
		t.assignAction(() -> cancelImpl(), null);
		return t;
	}
	
	private final class TCS implements TaskCompletionSource {

		@Override
		public boolean succeed() {
			return succeedImpl(null);
		}

		@Override
		public boolean fail(Throwable exception) {
			return failImpl(exception);
		}

		@Override
		public Task task() {
			return taskView;
		}
		
	}
	
	private final class TCSOf implements TaskCompletionSourceOf<T> {

		@Override
		public boolean succeed(T value) {
			return succeedImpl(value);
		}

		@Override
		public boolean fail(Throwable exception) {
			return failImpl(exception);
		}

		@Override
		public TaskOf<T> taskOf() {
			return taskOfView;
		}
		
	}
}
