package aa4j.test;

import aa4j.task.TaskAccess;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("javadoc")
public class AA4JTest {

	public static void main(String[] args) throws InterruptedException, ExecutionException, CancellationException, IllegalStateException, IOException {
		TaskAccess<String> taskAccess = Tasks.create(null);
		var task1 = taskAccess.taskOf();
		var task2 = Tasks.chain(task1, AA4JTest::parseIntAsync);
		var task2Alt = Tasks.map(task1, Integer::parseInt);
		
		taskAccess.tcsOf().succeed("1234asdf");
//		System.out.println(task1.cancel());
//		System.out.println(taskAccess.tcsOf().succeed("1234"));
//		taskAccess.tcs().fail(new CancellationException("Does not actually cancel the task!"));
		
		Tasks.report("task1", task1);
		Tasks.report("task2", task2);
		Tasks.report("task2Alt", task2Alt);
		
	}
	
	static TaskOf<Integer> parseIntAsync(String string) {
		return Tasks.success(Integer.valueOf(string));
	}
	
	static CompletableFuture<Integer> parseInt(String string) {
		//return CompletableFuture.failedFuture(new CancellationException());
		return new CompletableFuture<Integer>().completeOnTimeout(Integer.valueOf(string), 50, TimeUnit.MILLISECONDS);
	}
	
}
