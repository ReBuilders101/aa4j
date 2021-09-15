package aa4j.test;

import aa4j.task.TaskOf;
import static aa4j.AA4JStatic.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("javadoc")
public class AA4JTest {

	public static void main(String[] args) throws InterruptedException, ExecutionException, CancellationException, IllegalStateException, IOException {

		CompletableFuture<String> cpf = CompletableFuture.completedFuture("asdf");
		cpf.whenComplete((a,b) -> System.out.println(a + " - " + b));
		System.out.println(cpf.thenApply(str -> str.length()).get());
	}

	
	TaskOf<Integer> foo() {
		TaskOf<String> future = ((TaskOf<String>) null); 
		
		future.getResult(TO_UNCHECKED);
		
		return await(future, str -> {
			return Integer.valueOf(str);
		});
	}
	
}
