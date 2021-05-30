package aa4j.task;

/**
 * This exception is thrown when one attempts to invoke {@link CancellationToken#cancelOrFail()}
 * on a token that has already been used (where one of the {@code cancel} methods has been invoked before).
 */
public final class TokenUsedException extends Exception {
	private static final long serialVersionUID = 1463414592379632784L;

	/*package*/ TokenUsedException(String message) {
		super(message);
	}
	
}
