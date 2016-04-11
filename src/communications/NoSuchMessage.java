package communications;

public class NoSuchMessage extends IllegalStateException {
	
	private static final long serialVersionUID = 1L;
	
	public NoSuchMessage() {
		super();
	}
	
	public NoSuchMessage(String cause) {
		super(cause);
	}
}
