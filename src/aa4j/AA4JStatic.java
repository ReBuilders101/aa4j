package aa4j;

import java.util.function.Function;

import aa4j.task.TaskOf;

public final class AA4JStatic {
	private AA4JStatic() { throw new RuntimeException("No instance for you"); }
	
	public static <Awaited, Continued> TaskOf<Continued> await(TaskOf<Awaited> awaitedTask, Function<Awaited, Continued> continuation) {
		//TODO
		return null;
	}
	
	public static final Function<Throwable, RuntimeException> TO_UNCHECKED = RuntimeException::new;
	
	public static final Runnable NOOP = () -> {};
}
