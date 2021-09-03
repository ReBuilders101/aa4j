package aa4j.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import aa4j.CancellationStatusSupplier;

/**
 * Task associated with an action blocking another thread
 * @param <T> Type of task result
 */
/*package*/ final class BlockingTask<T> extends AbstractCompletableFutureTask<T> implements CancellationStatusSupplier {

	private volatile boolean cancellationRequested;
	
	protected BlockingTask(CompletableFuture<T> usedCpf, boolean canCancelFlag) {
		super(usedCpf, canCancelFlag);
		cancellationRequested = false;
	}

	@Override
	protected CancelResult cancelImpl() {
		if(getStateImpl().isDone()) return fromDoneState();
		
		if(isCancellableWhileRunning) {
			cancellationRequested = true;
			return CancelResult.CANCELLATION_PENDING;
		} else {
			return CancelResult.UNABLE_TO_CANCEL;
		}
	}
	
	protected boolean confirmCancelImpl() {
		if(cancellationRequested) {
			return cpf.completeExceptionally(new CancellationException());
		} else {
			throw new IllegalStateException("Cannot confirm cancellation: not requested");
		}
	}
	
	@Override
	public boolean isCancellationRequested() {
		return cancellationRequested;
	}

	@Override
	public TaskCompletionSourceOf<T> tcsOf() {
		throw new UnsupportedOperationException("BlockingTasks cannot be completed with TCS objects");
	}

	@Override
	public TaskCompletionSource tcs() {
		throw new UnsupportedOperationException("BlockingTasks cannot be completed with TCS objects");
	}
	
	private CancelResult fromDoneState() {
		var state = getStateImpl();
		if(!state.isDone()) throw new IllegalStateException("State is not done");
		if(state.isSuccess()) return CancelResult.ALREADY_SUCCEEDED;
		if(state.isFailed()) return CancelResult.ALREADY_FAILED;
		return CancelResult.ALREADY_CANCELLED;
	}
}
