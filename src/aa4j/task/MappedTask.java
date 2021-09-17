package aa4j.task;

import java.util.concurrent.CompletionStage;

/**
 * A task that has no completion logic on its own, it completes through another completion stage
 * @param <T> Result type
 */
class MappedTask<T> extends AbstractCompletionStageTask<T> {

	private final ChainedCancellationRequest ccrq;
	
	protected MappedTask(CompletionStage<T> mappedStage, ChainedCancellationRequest ccrq) {
		super(mappedStage);
		this.ccrq = ccrq;
	}

	@Override
	protected CancelResult cancelImpl() {
		return ccrq.cancel();
	}
	
	@FunctionalInterface
	/*package*/ static interface ChainedCancellationRequest {
		public CancelResult cancel();
	}
}
