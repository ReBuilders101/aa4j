package aa4j.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import aa4j.CancellationStatusSupplier;

/**
 * Task associated with an action blocking another thread
 * @param <T> Type of task result
 */
/*package*/ final class BlockingTask<T> extends AbstractCompletionStageTask<T> implements CancellationStatusSupplier {

	private volatile boolean cancellationRequested;
	
	protected BlockingTask(CompletableFuture<T> usedCpf, boolean canCancelFlag) {
		super(usedCpf, canCancelFlag);
		cancellationRequested = false;
	}

	@Override
	protected CancelResult cancelImpl() {
		if(getStateImpl().isDone()) return fromDoneState();
		
		if(isCancellable) {
			cancellationRequested = true;
			return CancelResult.CANCELLATION_PENDING;
		} else {
			return CancelResult.UNABLE_TO_CANCEL;
		}
	}
	
	protected boolean succeedImpl(T value) {
		return stage.complete(value);
	}
	
	protected boolean failImpl(Throwable ex) {
		return stage.completeExceptionally(wrapFailureReason(ex));
	}
	
	protected CancellationToken token() {
		var t = CancellationToken.unbound();
		t.assignAction(() -> cancelImpl(), null);
		return t;
	}
	
	protected boolean confirmCancelImpl() {
		if(cancellationRequested) {
			return stage.completeExceptionally(new CancellationException());
		} else {
			throw new IllegalStateException("Cannot confirm cancellation: not requested");
		}
	}
	
	@Override
	public boolean isCancellationRequested() {
		return cancellationRequested;
	}
}
