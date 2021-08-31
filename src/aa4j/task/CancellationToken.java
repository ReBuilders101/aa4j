package aa4j.task;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A single-use token that can be associated with some action and then used to cancel that action.
 */
public final class CancellationToken {

	private final AtomicReference<Runnable> binding;
	private final AtomicBoolean used;
	
	private CancellationToken() {
		this.binding = new AtomicReference<>(null);
		this.used 	 = new AtomicBoolean(false);
	}
	
	/**
	 * Whether this token currently has a bound action.<br>
	 * A return value of {@code true} guarantees that the token will also
	 * be bound in the future, while a return value of {@code false} might
	 * be immediately outdated when the token is accessed by more than one thread.
	 * @return {@code true} if an action is bound to this token, {@code false} if not
	 */
	public boolean isBound() {
		return this.binding.get() == null;
	}
	
	/**
	 * Whether this token was already used to cancel an action. If a token has already been used,
	 * the associated action will not receive any more cancellation requests.<br>
	 * A return value of {@code true} guarantees that this token will also have been used
	 * in the future, while a return value of {@code false} might
	 * be immediately outdated when the token is accessed by more than one thread.
	 * @return {@code true} if the cancellation request has already been made, {@code false} if not
	 */
	public boolean isUsed() {
		return this.used.get();
	}
	
	/**
	 * Sends a cancellation request to the associated action, or throws
	 * an exception if this token has already been used.
	 * If the request is made successfully, then this token will be marked as used.<br>
	 * The method does not guarantee that the cancellation request result in successful cancellation or is received
	 * at all, this behavior is defined by the associated action.
	 * @throws TokenUsedException When the token has already been used to send a cancellation request
	 * @throws IllegalStateException When the token is not bound to an action
	 */
	public void cancelOrFail() throws TokenUsedException, IllegalStateException {
		var snap = this.binding.get();
		if(snap == null) throw new IllegalStateException("CancellationToken is not bound to an action");
		if(!used.compareAndSet(false, true)) throw new TokenUsedException("CancellationToken has already been used");
		snap.run();
	}
	
	/**
	 * Sends a cancellation request to the associated action, or does nothing
	 * if the token has already been used.
	 * If the request is made successfully, then this token will be marked as used.<br>
	 * The method does not guarantee that the cancellation request result in successful cancellation or is received
	 * at all, this behavior is defined by the associated action.
	 * @throws IllegalStateException When the token is not bound to an action
	 */
	public void cancel() throws IllegalStateException {
		var snap = this.binding.get();
		if(snap == null) throw new IllegalStateException("CancellationToken is not bound to an action");
		if(!used.compareAndSet(false, true)) return; //Just ignore that here
		snap.run();
	}
	
	/**
	 * Sends a cancellation request to the associated action if the token is bound to
	 * an action, or invalidates the token by assigning an empty action before cancelling.
	 */
	public void cancelOrInvalidate() {
		if(!this.binding.compareAndSet(null, () -> {})) { //If true, it was just invalidated. Now no action can be assigned
			//if false, it was already bound, and that action has to be cancelled
			var snap = this.binding.get();
			if(!used.compareAndSet(false, true)) return; //Already cancelled
			snap.run();
		} else { //invalidated, mark as cancelled (no need to run the NOOP)
			used.set(true);
		}
	}
	
	/**
	 * Assigns an action to this token.
	 * @param action The assigned action
	 * @param ex the exception when the token is bound
	 * @throws ex 
	 */
	/*package*/ void assignAction(Runnable action, Supplier<? extends RuntimeException> ex){
		if(!this.binding.compareAndSet(null, action)) {
			throw ex.get();
		}
	}
	
	/**
	 * Creates a token that is not bound to any action.
	 * @return A new, unbound {@link CancellationToken}
	 */
	public static CancellationToken unbound()  {
		return new CancellationToken();
	}
	
	/**
	 * Creates a token that is bound to a tasks cancellation action ({@link TaskOf#cancel()}).<br>
	 * The created token provides fewer features than the actual {@code cancel} method on the task,
	 * as the returned value is ignored.
	 * @param task The task for which the token should be created
	 * @return A new, bound {@link CancellationToken}
	 */
	public static CancellationToken taskToken(TaskOf<?> task) {
		var t = new CancellationToken();
		t.assignAction(task::cancel,() -> null);
		return t;
	}
	
	/**
	 * Creates a token that is bound to a tasks cancellation action ({@link Task#cancel()}).<br>
	 * The created token provides fewer features than the actual {@code cancel} method on the task,
	 * as the returned value is ignored.
	 * @param task The task for which the token should be created
	 * @return A new, bound {@link CancellationToken}
	 */
	public static CancellationToken taskToken(Task task) {
		var t = new CancellationToken();
		t.assignAction(task::cancel, () -> null);
		return t;
	}
	
	/**
	 * Creates a token that is bound to an empty action. Its only feature
	 * is the {@link #isUsed()} flag, which can be flipped to {@code true}
	 * by invoking any of the {@code cancel} methods.
	 * @return A new, bound {@link CancellationToken}
	 */
	public static CancellationToken stateToken() {
		var t = new CancellationToken();
		t.assignAction(() -> {}, () -> null);
		return t;
	}
}
