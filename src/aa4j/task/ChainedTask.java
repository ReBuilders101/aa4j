package aa4j.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import aa4j.task.MappedTask.ChainedCancellationRequest;

class ChainedTask<T> extends AbstractCompletionStageTask<T> {

	private static final int STAGE_1 = 0; //executing stage 1
	private static final int CRQ = 1; //cancellation requested before stage 1 completed
	private static final int CHECKED = 2; //cancellation state checked, will push task2ccrq soon
	
	private final StateHolder state;
	
	private ChainedTask(StateHolder state, CompletionStage<T> stage) {
		super(stage);
		this.state = state;
	}

	@Override
	protected CancelResult cancelImpl() {
		if(state.cancellationState.compareAndSet(STAGE_1, CRQ)) {
			var result = state.task1ccrq.cancel();
			return result.isCancelledByAction() ? result : CancelResult.CANCELLATION_PENDING;
		} else { //Either already cancelled or checked
			if(state.cancellationState.get() == CHECKED) {
				//Spin-wait until the second ccrq is filled in
				while(state.task2ccrq == null) Thread.onSpinWait();
				return state.task2ccrq.cancel();
			} else { //CRQ active -> pending
				return CancelResult.CANCELLATION_PENDING;
			}
		}
	}

	
	
	private static <U, T> Function<U, CompletionStage<T>> futureSupplier(Function<U, TaskOf<T>> chainedTask, StateHolder state) {
		return u -> {
			//first, check cancellation state
			final var cancelCode = state.cancellationState.getAndSet(CHECKED);
			if(cancelCode == CRQ) {
				state.task2ccrq = () -> CancelResult.ALREADY_CANCELLED;
				throw new CancellationException(); //Don't even try to start the next stage
			} else if(cancelCode == STAGE_1) {
				//We are good to go
				final var task = chainedTask.apply(u);
				state.task2ccrq = task::cancel;
				return task.stage();
			} else { //Alerady CHECKED???
				throw new Error("Invalid ChainedTask cancellation state");
			}
		};
	}
	
	/**
	 * This is required instead of a constructor, because a StateHolder needs to exist and be initialized
	 * before the super constructor is called.
	 */
	protected static <U, T> ChainedTask<T> create(TaskOf<U> task, Function<U, TaskOf<T>> chainedTask) {
		final var sh = new StateHolder(task);
		final var sup = futureSupplier(chainedTask, sh);
		return new ChainedTask<>(sh, task.stage().thenCompose(sup));
	}
	
	private static final class StateHolder {
		private final ChainedCancellationRequest task1ccrq;
		private volatile ChainedCancellationRequest task2ccrq;
		private final AtomicInteger cancellationState;
		
		private StateHolder(TaskOf<?> task1) {
			this.task1ccrq = task1::cancel;
			this.task2ccrq = null;
			this.cancellationState = new AtomicInteger(STAGE_1);
		}
	}
}
