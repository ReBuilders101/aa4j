package aa4j.function;

import aa4j.CancellationConfirmed;
import aa4j.CancellationStatusSupplier;

@FunctionalInterface
public interface CancellableSyncAction<PARAMETER, RESULT> {

	public RESULT syncAction(PARAMETER param, CancellationStatusSupplier tcss) throws CancellationConfirmed;
	
}
