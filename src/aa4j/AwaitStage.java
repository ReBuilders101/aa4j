package aa4j;

import aa4j.function.AsyncAction;
import aa4j.function.CancellableAsyncAction;
import aa4j.function.CancellableSyncAction;
import aa4j.function.SyncAction;
import aa4j.task.TaskOf;

/*package*/ class AwaitStage<T> implements OnAwaitActions<T>, OnSyncActions<T> {
	
	//can this stage be cancelled
	public boolean supportsCancellation() {
		
	}
	
	//can any following stage be cancelled
	public boolean supportsFutureCancellation() {
		
	}
	
	@Override
	public <FUNC_RESULT> OnAwaitActions<FUNC_RESULT> await(AsyncAction<T, FUNC_RESULT> action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <FUNC_RESULT> OnAwaitActions<FUNC_RESULT> await(CancellableAsyncAction<T, FUNC_RESULT> action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskOf<T> task() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <FUNC_RESULT> OnSyncActions<FUNC_RESULT> sync(SyncAction<T, FUNC_RESULT> action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <FUNC_RESULT> OnSyncActions<FUNC_RESULT> sync(CancellableSyncAction<T, FUNC_RESULT> action) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
