package aa4j;

import aa4j.function.AsyncAction;
import aa4j.function.SyncAction;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

/*package*/ class AwaitStage<T> implements OnAwaitActions<T> {
	
	private final TaskOf<T> stageTask;
	
	protected AwaitStage(TaskOf<T> task) {
		stageTask = task;
	}
	
	@Override
	public <FUNC_RESULT> OnAwaitActions<FUNC_RESULT> await(AsyncAction<T, FUNC_RESULT> action) {
		return new AwaitStage<>(Tasks.chain(stageTask, t -> action.asyncAction(t)));
	}

	@Override
	public TaskOf<T> task() {
		return stageTask;
	}

	@Override
	public <FUNC_RESULT> OnSyncActions<FUNC_RESULT> sync(SyncAction<T, FUNC_RESULT> action) {
		return new AwaitStage<>(Tasks.map(stageTask, action::syncAction)); 
	}
	
}
