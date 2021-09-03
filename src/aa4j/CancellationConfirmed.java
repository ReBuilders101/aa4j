package aa4j;

/**
 * {@code CancellationConfirmed} is a {@link Throwable} (and not an {@link Exception}) that is thrown by
 * {@link CancellationStatusSupplier#allowCancellation()} to end the running task an move up in the callstack to a point where
 * this object is caught by the task driver.
 * <p><b>
 * This should not be thrown or caught manually.
 * </p></b>
 */
public class CancellationConfirmed extends Throwable {
	private static final long serialVersionUID = 1762293670443363132L;
	/*package*/ CancellationConfirmed() {
		super("aa4j error: this type of exception should have been caught.");
	}
}
