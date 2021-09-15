package aa4j.task;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class ChainedTask<T> extends AbstractCompletionStageTask<T> {

	protected <U> ChainedTask(CompletableFuture<U> future, Function<U, CompletableFuture<T>> chainedFuture, boolean cancellable) {
		super(null, cancellable);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected CancelResult cancelImpl() {
		// TODO Auto-generated method stub
		return null;
	}

}
