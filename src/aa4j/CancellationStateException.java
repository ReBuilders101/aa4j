package aa4j;

/**
 * Thrown when calling {@link CancellationStatusSupplier#allowCancellation()} when cancellation was not requested.
 */
public class CancellationStateException extends Exception {
	private static final long serialVersionUID = -5247035881770939593L;

	/*package*/ CancellationStateException() {
		super("Task cancellation was not requested, cannot generate CancellationConfirmed object");
	}
	
}
