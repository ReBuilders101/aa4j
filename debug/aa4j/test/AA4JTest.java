package aa4j.test;

import aa4j.task.Task;
import aa4j.task.TaskAccess;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

import static aa4j.AA4JStatic.*;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("javadoc")
public class AA4JTest {

	public static void main(String[] args) throws InterruptedException, ExecutionException, CancellationException, IllegalStateException, IOException {
		CompletableFuture<String> f = new CompletableFuture<>();
		f.whenComplete((s,t) -> System.out.println("value: " + s + " ex: " + t));
		f.exceptionally((t) -> {
			System.out.println("Error: " + t);
			return "Hi";
		});
//		f.completeExceptionally(new ExecutionException(null));
//		f.cancel(true);
		f.complete("good");
		
		var f2 = CompletableFuture.completedFuture("UwU");
		f2.whenComplete((a,b) -> System.out.println("Afterwards"));
		System.out.println("get-f2:" + f2.get());
		
		
		System.out.println(f.get());
		System.out.println("Now with tasks");
		
		final TaskAccess<String> acc = Tasks.manualBlocking();
		acc.tcsOf().succeed("Result");
		var res = acc.taskOf().getResult(IOException.class, TO_UNCHECKED);
		
		
		final Task t = Tasks.delayCancellable(5, TimeUnit.SECONDS); 
		Tasks.delay(3, TimeUnit.SECONDS).whenDone(() -> t.cancel());
		
		System.out.println("Before: " + System.currentTimeMillis());
		t.awaitSuccess();
		System.out.println("After: " + System.currentTimeMillis() + " With " + t.getState());
	}
	
	
	
	TaskOf<Integer> foo() {
		TaskOf<String> future = ((TaskOf<String>) null); 
		
		future.getResult(TO_UNCHECKED);
		
		return await(future, str -> {
			return Integer.valueOf(str);
		});
	}
	
}
