package aa4j;

import aa4j.function.CancellableSyncAction;
import aa4j.function.SyncAction;

/**
 * Interface for exposing actions that can be taken after awaiting a task of {@code RESULT} type.
 * @param <TYPE> The result type of the task that was awaited
 */
public interface OnAwaitActions<TYPE> extends OnSyncActions<TYPE> {

	public <FUNC_RESULT> OnSyncActions<FUNC_RESULT> sync(SyncAction<TYPE, FUNC_RESULT> action);
	public <FUNC_RESULT> OnSyncActions<FUNC_RESULT> sync(CancellableSyncAction<TYPE, FUNC_RESULT> action);
	
}
