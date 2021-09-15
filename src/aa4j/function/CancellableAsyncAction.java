package aa4j.function;

import aa4j.CancellationConfirmed;
import aa4j.CancellationStatusSupplier;
import aa4j.task.TaskOf;

@FunctionalInterface
public interface CancellableAsyncAction<PARAMETER, RESULT> {

	public TaskOf<RESULT> asyncAction(PARAMETER param, CancellationStatusSupplier tcss) throws CancellationConfirmed;
	
}
