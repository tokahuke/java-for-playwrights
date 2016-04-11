package communications;



public class TimeoutException extends RxException {
	private static final long serialVersionUID = 1952L;
	
	public TimeoutException(String protocolName, String messageName, long runId) {
		super(String.format("Message %s in protocol %s, run %s timed out.",
				messageName, protocolName, ShortMessage.idToString(runId)));
	}
}
