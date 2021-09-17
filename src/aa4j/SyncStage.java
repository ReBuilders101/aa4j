package aa4j;

import java.util.function.Supplier;

import aa4j.function.AsyncAction;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

/*package*/ class SyncStage<T> implements OnSyncActions<T> {

	private final Supplier<T> stageValue;
	
	public SyncStage(Supplier<T> value) {
		stageValue = value;
	}

	@Override
	public <FUNC_RESULT> OnAwaitActions<FUNC_RESULT> await(AsyncAction<T, FUNC_RESULT> action) {
		return new AwaitStage<>(Tasks.chain(task(), action::asyncAction));
	}

	@Override
	public TaskOf<T> task() {
		try {
			var v = stageValue.get();
			return Tasks.success(v);
		} catch (Exception e) {
			return Tasks.failure(e);
		}
	}
}
