package communications;

public class TxException extends RuntimeException {
	private static final long serialVersionUID = -753L;
	
	public TxException() {}
	
	public TxException(String cause) {
		super(cause);
	}
	
	public TxException(Throwable cause) {
		super(cause);
	}
}
