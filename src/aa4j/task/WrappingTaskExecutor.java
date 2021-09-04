package aa4j.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import aa4j.TaskExecutorService;
import aa4j.function.ActiveCancellableTask;
import aa4j.function.ActiveCancellableTaskOf;
import aa4j.function.ActiveTask;
import aa4j.function.ActiveTaskOf;

class WrappingTaskExecutor implements TaskExecutorService {

	private final ExecutorService delegate;
	private final List<CancellationToken> tcts;
	
	private final TaskAccess<?> awt;
	private final AtomicBoolean threadStarted;
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	
	public WrappingTaskExecutor(ExecutorService delegateExecutor) {
		delegate = Objects.requireNonNull(delegateExecutor, "'delegateExecutor' parameter must not be null");
		awt = Tasks.manualBlocking();
		tcts = Collections.synchronizedList(new LinkedList<>());
		threadStarted = new AtomicBoolean(false);
	}
	
	private void ensureThreadStarted() {
		if(threadStarted.compareAndSet(false, true)) {
			var t = new Thread(() -> {
				try {
					if(delegate.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
						awt.tcs().succeed();
					} else {
						awt.tcs().fail(new TimeoutException("Max waiting time elapsed before ExecutorService terminated"));
					}
				} catch (InterruptedException e) {
					awt.tcs().fail(e);
				}
			});
			t.setName("WrappingTaskExecutor-Termination-Watcher-" + THREAD_ID.getAndIncrement());
			t.setDaemon(true);
			t.start();
		}
	}
	
	@Override
	public Task getTerminationTask() {
		ensureThreadStarted();
		return awt.task();
	}
	
	@Override
	public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(! delegate.awaitTermination(timeout, unit)) {
			throw new TimeoutException();
		}
	}
	
	@Override
	public void execute(Runnable command) {
		delegate.execute(command);
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public Task submit(ActiveTask task) {
		if(delegate.isShutdown()) throw new RejectedExecutionException("Delegate ExecutorService is shut down");
		
		final BlockingTask<Void> t = new BlockingTask<>(new CompletableFuture<>(), false);
		final CancellationToken tct = t.newToken();
		final TaskDriver<?> d = new TaskDriver<Void>(t, task) {
			@Override 
			protected void postTaskDone(TaskState state) {
				tcts.remove(tct);
			}
		};
		
		//Add cancellation token. it will be removed after task completion by postTaskDone or when execution is rejected
		tcts.add(tct);
		
		try {
			delegate.execute(d);
		} catch (RejectedExecutionException e) {
			tcts.remove(tct);
			throw e;
		}
		return t.task;
	}

	@Override
	public Task submit(ActiveCancellableTask task) {
		if(delegate.isShutdown()) throw new RejectedExecutionException("Delegate ExecutorService is shut down");
		
		final BlockingTask<Void> t = new BlockingTask<>(new CompletableFuture<>(), false);
		final CancellationToken tct = t.newToken();
		final TaskDriver<?> d = new TaskDriver<Void>(t, task) {
			@Override 
			protected void postTaskDone(TaskState state) {
				tcts.remove(tct);
			}
		};
		
		//Add cancellation token. it will be removed after task completion by postTaskDone or when execution is rejected
		tcts.add(tct);
		
		try {
			delegate.execute(d);
		} catch (RejectedExecutionException e) {
			tcts.remove(tct);
			throw e;
		}
		return t.task;
	}

	@Override
	public <T> TaskOf<T> submit(ActiveTaskOf<T> task) {
		if(delegate.isShutdown()) throw new RejectedExecutionException("Delegate ExecutorService is shut down");
		
		final BlockingTask<T> t = new BlockingTask<>(new CompletableFuture<>(), false);
		final CancellationToken tct = t.newToken();
		final TaskDriver<T> d = new TaskDriver<T>(t, task) {
			@Override 
			protected void postTaskDone(TaskState state) {
				tcts.remove(tct);
			}
		};
		
		//Add cancellation token. it will be removed after task completion by postTaskDone or when execution is rejected
		tcts.add(tct);
		
		try {
			delegate.execute(d);
		} catch (RejectedExecutionException e) {
			tcts.remove(tct);
			throw e;
		}
		return t.taskOf;
	}

	@Override
	public <T> TaskOf<T> submit(ActiveCancellableTaskOf<T> task) {
		if(delegate.isShutdown()) throw new RejectedExecutionException("Delegate ExecutorService is shut down");
		
		final BlockingTask<T> t = new BlockingTask<>(new CompletableFuture<>(), false);
		final CancellationToken tct = t.newToken();
		final TaskDriver<T> d = new TaskDriver<T>(t, task) {
			@Override 
			protected void postTaskDone(TaskState state) {
				tcts.remove(tct);
			}
		};
		
		//Add cancellation token. it will be removed after task completion by postTaskDone or when execution is rejected
		tcts.add(tct);
		
		try {
			delegate.execute(d);
		} catch (RejectedExecutionException e) {
			tcts.remove(tct);
			throw e;
		}
		return t.taskOf;
	}

	@Override
	public int shutdownNow() {
		synchronized (tcts) {
			tcts.forEach(CancellationToken::cancel);
			return tcts.size();
		}
	}
}
