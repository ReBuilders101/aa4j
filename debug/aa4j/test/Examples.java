package aa4j.test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import aa4j.task.TaskAccess;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

public class Examples {

	public static TaskOf<Integer> ping(InetAddress address, int timeout) {
		Objects.requireNonNull(address, "'address' parameter cannot be null");
		if(timeout < 0) throw new IllegalArgumentException("'timeout' parameter cannot be negative"); 
		
		TaskAccess<Integer> res = Tasks.manualBlocking();
		new Thread(() -> {
			try {
				long start = System.currentTimeMillis();
				if(address.isReachable(timeout)) {
					res.tcsOf().succeed((int) (System.currentTimeMillis() - start));
				} else {
					res.tcs().fail(new TimeoutException("Timed out before png was returned"));
				}
			} catch (IOException e) {
				res.tcs().fail(e);
			}
		}).run();
		return res.taskOf();
	}
	
}
