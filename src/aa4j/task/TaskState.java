package aa4j.task;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This enumeration contains all possible states that a {@link TaskOf} can have.
 */
public enum TaskState {

	//So we cant't refer to the phase constants here, because they are declared later,
	//but we also cannot declare the before the enum constants. Magic numbers to the rescue!
	
	/**
	 * The task has been started and some action is currently executing.<br>
	 * The flag methods will return the following values:
	 * <table>
	 *   <tr><th>Method</th>                 <th>Value</th></tr>
	 *   <tr><td>{@link #isRunning()}</td>   <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isDone()}</td>      <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isSuccess()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isCancelled()}</td> <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isFailed()}</td>    <td>{@code false}</td></tr>
	 *   <tr><td>{@link #phase()}</td>       <td>{@code 1}</td></tr>
	 * </table>
	 */
	RUNNING(1),
	/**
	 * The task has finished executing successfully.<br>
	 * The flag methods will return the following values:
	 * <table>
	 *   <tr><th>Method</th>                 <th>Value</th></tr>
	 *   <tr><td>{@link #isRunning()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isDone()}</td>      <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isSuccess()}</td>   <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isCancelled()}</td> <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isFailed()}</td>    <td>{@code false}</td></tr>
	 *   <tr><td>{@link #phase()}</td>       <td>{@code 2}</td></tr>
	 * </table>
	 */
	SUCCEEDED(2),
	/**
	 * The task has finished executing because of an exception.<br>
	 * The flag methods will return the following values:
	 * <table>
	 *   <tr><th>Method</th>                 <th>Value</th></tr>
	 *   <tr><td>{@link #isRunning()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isDone()}</td>      <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isSuccess()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isCancelled()}</td> <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isFailed()}</td>    <td>{@code true}</td></tr>
	 *   <tr><td>{@link #phase()}</td>       <td>{@code 2}</td></tr>
	 * </table>
	 */
	FAILED(2),
	/**
	 * The task has finished executing because it was cancelled while running.<br>
	 * The flag methods will return the following values:
	 * <table>
	 *   <tr><th>Method</th>                 <th>Value</th></tr>
	 *   <tr><td>{@link #isRunning()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isDone()}</td>      <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isSuccess()}</td>   <td>{@code false}</td></tr>
	 *   <tr><td>{@link #isCancelled()}</td> <td>{@code true}</td></tr>
	 *   <tr><td>{@link #isFailed()}</td>    <td>{@code false}</td></tr>
	 *   <tr><td>{@link #phase()}</td>       <td>{@code 2}</td></tr>
	 * </table>
	 */
	CANCELLED(2);
	
	private final int phase;
	
	/**
	 * Phase number (1) for the running phase
	 * ({@link #RUNNING}).
	 * @see #phase()
	 */
	public static final int PHASE_RUN = 1;
	/**
	 * Phase number (2) for the finished phase
	 * ({@link #SUCCEEDED}, {@link #FAILED}, {@link #CANCELLED}).
	 * @see #phase()
	 */
	public static final int PHASE_DONE = 2;
	
	private TaskState(int phase) {
		this.phase = phase;
	}
	
	/**
	 * This method will return {@code true} only if the task is currently running,
	 * not if it is initialized or done.
	 * @return {@code true} if the task is currently running, {@code false} if not
	 */
	public boolean isRunning() {
		return this.phase == PHASE_RUN;
	}
	
	/**
	 * This method will return {@code true} if the task has completed in any way,
	 * successfully, by failure or by cancellation.
	 * @return {@code true} if the task is done in any way, {@code false} if not
	 */
	public boolean isDone() {
		return this.phase == PHASE_DONE;
	}
	
	/**
	 * This method will return {@code true} only if the task is done and has completed successfully.
	 * @return {@code true} if the task was successful, {@code false} otherwise
	 */
	public boolean isSuccess() {
		return this == SUCCEEDED;
	}
	
	/**
	 * This method will return {@code true} only if the task was cancelled before successful completion.
	 * @return {@code true} if the task was cancelled, {@code false} otherwise
	 */
	public boolean isCancelled() {
		return this == CANCELLED;
	}
	
	/**
	 * This method will return {@code true} only if the task has failed with an exception.
	 * @return {@code true} if the task has failed, {@code false} otherwise
	 */
	public boolean isFailed() {
		return this == FAILED;
	}
	
	/**
	 * The phase number of this state. The phase number restricts the way a tasks state can change during its
	 * lifetime: For every task state change the phase number must increase.<br>All tasks start with a phase of
	 * 1 ({@link #PHASE_RUN}), and they always end with a state of 2 ({@link #PHASE_DONE}).
	 * @return The phase number for this state
	 */
	public int phase() {
		return phase;
	}
	
	/**
	 * Performs a compare-and-set on the provided 
	 * {@link AtomicReference} only if the change is valid considering state phase numbers.<br>
	 * Parameters are expected no be not {@code null}.
	 * @param reference The reference to perform the CAS on
	 * @param expectedState The expected state of the reference
	 * @param newState The new state that should be assingned to the reference
	 * @return {@code true} if the CAS was successful, {@code false} if expected and actual value do not match
	 * @throws IllegalStateException If the state transition is illegal considering the phase numbers 
	 */
	public static boolean checkedCAS(AtomicReference<TaskState> reference, TaskState expectedState, TaskState newState) throws IllegalStateException {
		if(expectedState.phase >= newState.phase) 
			throw new IllegalArgumentException("Cannot move from state " + expectedState + " (Phase " + expectedState.phase
					+ ") to " + newState + " (Phase " + newState.phase + ") because new phase number is not larger");
		return reference.compareAndSet(expectedState, newState);
	}
}
