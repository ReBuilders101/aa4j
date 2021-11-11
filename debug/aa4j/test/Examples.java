package aa4j.test;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import aa4j.AA4J;
import aa4j.task.TaskAccess;
import aa4j.task.TaskOf;
import aa4j.task.Tasks;

public class Examples {

	public static TaskOf<Integer> ping(InetAddress address, int timeout) {
		Objects.requireNonNull(address, "'address' parameter cannot be null");
		if(timeout < 0) throw new IllegalArgumentException("'timeout' parameter cannot be negative"); 
		
		TaskAccess<Integer> res = Tasks.create();
		new Thread(() -> {
			try {
				long start = System.currentTimeMillis();
				if(address.isReachable(timeout)) {
					res.tcsOf().succeed((int) (System.currentTimeMillis() - start));
				} else {
					res.tcs().fail(new TimeoutException("Timed out before ping was returned"));
				}
			} catch (IOException e) {
				res.tcs().fail(e);
			}
		}).run();
		return res.taskOf();
	}
	
	
	public static TaskOf<SocketChannel> connectChannel(InetAddress address) {
		return null;
	}
	
	public static TaskOf<Object> receiveData(SocketChannel channel) {
		return null;
	}
	
	public static TaskOf<Object> foo() {
		//Sync code generates address
		InetAddress inetaddress = null;

		return AA4J
				.sync(() -> inetaddress)
				.await(address -> connectChannel(address))
				.await(channel -> receiveData(channel))
				.task();
	}
	
	public static TaskOf<Object> bar() {
		//Sync code generates address
		InetAddress inetaddress = null;
		
		return AA4J
			   .await(connectChannel(inetaddress))
			   .await(channel -> receiveData(channel))
			   .task();
	}
}
