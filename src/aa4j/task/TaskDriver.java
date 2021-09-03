package aa4j.task;

import aa4j.CancellationConfirmed;
import aa4j.function.ActiveCancellableTask;
import aa4j.function.ActiveCancellableTaskOf;
import aa4j.function.ActiveTask;
import aa4j.function.ActiveTaskOf;

/**
 * Contains code that manages active tasks
 */
class TaskDriver<T> implements Runnable {
	
	private final BlockingTask<T> task; 
	private final ActiveCancellableTaskOf<T> act;
	
	public TaskDriver(BlockingTask<T> task, ActiveCancellableTaskOf<T> act) {
		this.task = task;
		this.act = act;
	}
	
	public TaskDriver(BlockingTask<T> task, ActiveTaskOf<T> act) {
		this.task = task;
		this.act = tcss -> act.runTask();
	}

	public TaskDriver(BlockingTask<T> task, ActiveCancellableTask act) {
		this.task = task;
		this.act = tcss -> {act.runTask(tcss); return null;};
	}
	
	public TaskDriver(BlockingTask<T> task, ActiveTask act) {
		this.task = task;
		this.act = tcss -> {act.runTask(); return null;};
	}
	
	@Override
	public void run() {
		try {
			var res = act.runTask(task);
			
			if(!task.succeedImpl(res))  //now this should just not happen
				throw new IllegalStateException("Cannot complete BlockingTask: already completed with "
						+ task.getStateImpl());
		} catch (CancellationConfirmed e) {
			if(!task.confirmCancelImpl()) //We are considering cases that should actually never happen
				throw new IllegalStateException("Cannot confirm cancellation for BlockingTask: already completed with "
						+ task.getStateImpl());
		} catch (Exception e) {
			if(!task.failImpl(e)) //It's like a unit test, but in production code
				throw new IllegalStateException("Cannot fail BlockingTask: already completed with " + task.getStateImpl());
		}
	}

}
