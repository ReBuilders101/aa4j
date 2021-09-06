package aa4j.task;

import java.util.concurrent.CompletableFuture;

/**
 * A task that has no completion logic on its own, it completes through another completion stage
 * @param <T> Result type
 */
class MappedTask<T> extends AbstractCompletionStageTask<T> {

	private final ChainedCancellationRequest ccrq;
	
	protected MappedTask(CompletableFuture<T> mappedStage, ChainedCancellationRequest ccrq, boolean cancellable) {
		super(mappedStage, cancellable);
		this.ccrq = ccrq;
	}
	
	@FunctionalInterface
	/*package*/ static interface ChainedCancellationRequest {
		public CancelResult cancel();
	}

	@Override
	protected CancelResult cancelImpl() {
		return ccrq.cancel();
	}
}
