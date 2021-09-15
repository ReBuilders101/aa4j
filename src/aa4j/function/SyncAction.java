package aa4j.function;

@FunctionalInterface
public interface SyncAction<PARAMETER, RESULT> {

	public RESULT syncAction(PARAMETER param);
	
}
