package aa4j.task;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a collection of {@link CancellationToken}s that can all be cancelled simultaneously.
 */
public class CancellationTokenGenerator {

	private final Queue<CancellationToken> activeTokens;
	private final Lock lock;
	
	/**
	 * Creates a new {@link CancellationTokenGenerator}.
	 */
	public CancellationTokenGenerator() {
		activeTokens = new LinkedList<>();
		lock = new ReentrantLock();
	}
	
	/**
	 * Cancels all tokens that have been created through {@link #newUnboundToken()}.
	 * It is still possible to create new tokens afterwards, but the value of {@link #getTokenCount()}
	 * will be reset to zero.
	 */
	public void cancelAll() {
		lock.lock();
		try {
			for(var token : activeTokens) {
				token.cancelOrInvalidate();
			}
			activeTokens.clear();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * The amount of tokens that have been created with {@link #newUnboundToken()} sinc the last invocation
	 * of {@link #cancelAll()}. This is the number of tokens which will be cancelled with the next call to
	 * {@link #cancelAll()}.
	 * @return The amount of currently active tokens
	 */
	public int getTokenCount() {
		return activeTokens.size();
	}
	
	/**
	 * Creates a new {@link CancellationToken} that is not bound to any action and registers
	 * this token to be cancelled when {@link #cancelAll()} is called.
	 * @return THe created token
	 */
	public CancellationToken newUnboundToken() {
		var token = CancellationToken.unbound();
		lock.lock();
		try {
			activeTokens.add(token);
		} finally {
			lock.unlock();
		}
		return token;
	}
	
}
