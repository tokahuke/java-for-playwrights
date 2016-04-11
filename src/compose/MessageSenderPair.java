package compose;

class MessageSenderPair<M> {
	final M message;
	final String sender;
	
	public MessageSenderPair(M message, String sender) {
		super();
		this.message = message;
		this.sender = sender;
	}
}