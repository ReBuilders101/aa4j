package aa4j;

import aa4j.function.AsyncAction;
import aa4j.task.TaskOf;

public interface OnSyncActions<TYPE> {

	public <FUNC_RESULT> OnAwaitActions<FUNC_RESULT> await(AsyncAction<TYPE, FUNC_RESULT> action);
	
	public TaskOf<TYPE> task();
}
