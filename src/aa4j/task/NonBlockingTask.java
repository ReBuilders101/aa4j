package aa4j.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * A task that represents waiting for some action to occur that completes the task.
 * @param <T> The result type of the task
 */
/*package*/ final class NonBlockingTask<T> extends AbstractCompletableFutureTask<T>{
	
	/**
	 * @param usedCpf The {@link CompletableFuture} for this task
	 * @param startHandler A method to run to start the task. If null, task will be started
	 * @param cancelHandler A method to run when trying to cancel this task . If not null, Cancellation MUST be successful.
	 * @param runHandlerWhenPrevented Run cancel handler when task never started
	 * BUT: handler will only run when cancellation is happening
	 */
	protected NonBlockingTask(CompletableFuture<T> usedCpf, Runnable cancelHandler) {
		super(usedCpf, cancelHandler != null);
		this.cpf.whenComplete(whenCancelledImpl(cancelHandler)); //Run that automatically when cancelling
	}

	@Override
	protected CancelResult cancelImpl() {
		if(isCancellableWhileRunning) { //The easy case. We can cancel whenever
			if(cpf.completeExceptionally(new CancellationException())) {
				return CancelResult.SUCCESSFULLY_CANCELLED;
			} else { //It better be done then
				return fromDoneState();
			}
		} else { //cannot even try to cancel, either unable or already 
			return getStateImpl().isDone() ? fromDoneState() : CancelResult.UNABLE_TO_CANCEL;
		}
	}

	private CancelResult fromDoneState() {
		var state = getStateImpl();
		if(!state.isDone()) throw new IllegalStateException("State is not done");
		if(state.isSuccess()) return CancelResult.ALREADY_SUCCEEDED;
		if(state.isFailed()) return CancelResult.ALREADY_FAILED;
		return CancelResult.ALREADY_CANCELLED;
	}
}
