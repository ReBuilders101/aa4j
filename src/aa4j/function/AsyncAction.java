package aa4j.function;

import aa4j.task.TaskOf;

@FunctionalInterface
public interface AsyncAction<PARAMETER, RESULT> {

	public TaskOf<RESULT> asyncAction(PARAMETER param);
	
}
