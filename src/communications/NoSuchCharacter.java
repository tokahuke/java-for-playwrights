package communications;

public class NoSuchCharacter extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	
	public NoSuchCharacter() {
		super();
	}
	
	public NoSuchCharacter(String cause) {
		super(cause);
	}
}
