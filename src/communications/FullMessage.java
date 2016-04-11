package communications;

import java.math.BigInteger;
import java.util.Base64;
import java.util.function.Function;


public final class FullMessage<PayloadT> {
	
	// TODO is this the best way? To basically clone ShortMessage here? The
	// other way, one takes less code. But is there anything to lose? I really
	// do not remember why I did this choice.
	
	// Fields:
	
	/**
	 * The name of the protocol this message belongs to.
	 */
	private final String protocol;
	
	/**
	 * The id of the protocol round this message belongs to.
	 */
	private final long id;
	
	/**
	 * The name of the message.
	 */
	private final String name;
	
	/**
	 * The source address of this message.
	 */
	private String from;
	
	/**
	 *  The content of the message.
	 */
	private final PayloadT payload;
	
	
	//Constructor:
	
	/**
	 * Creates a new FullMessage object.
	 * 
	 * @param id
	 *            the id of the protocol round the message belongs to.
	 * @param protocol
	 *            the name of the protocol the message belongs to.
	 * @param name
	 *            the name of the message (e.g, "ACK", "request", ...)
	 * @param from
	 *            the source address of the message.
	 * @param payload
	 *            the content of the message.
	 */
	public FullMessage(long id, String protocol, String name, String from,
			PayloadT payload) {
		super();
		this.id = id;
		this.protocol = protocol;
		this.name = name;
		this.from = from;
		this.payload = payload;
	}

	
	// Getters (mainly) and setters:
	
	// TODO known issue: the methods return references to the original objects,
	// not copies of them. How can this be a security risk?
	
	/**
	 * Gets the id of the protocol round of the message.
	 * 
	 * @return a long representing the id.
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Gets the protocol name of the message.
	 * 
	 * @return a String with the name of the protocol.
	 */
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * Gets the name, also known as type, of the message.
	 * 
	 * @return a String with the name of the message.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the content of the message.
	 * 
	 * @return a reference to the message content.
	 */
	public PayloadT getPayload() {
		return payload;
	}
	
	/**
	 * Gets the source address of the message.
	 * 
	 * @return a String representing the source address.
	 */
	public String getFrom() {
		return from;
	}
	
	/**
	 * Sets the source address of the message.
	 * 
	 * @param from
	 *            a String representing the source address.
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	
	/**
	 * Gives the equivalent {@link ShortMessage} object of the current message.
	 * 
	 * @return a new ShortMessage object with the same fields as the current
	 *         message.
	 * @see ShortMessage#lengthen(String)
	 */
	public ShortMessage<PayloadT> shorten() {
		return new ShortMessage<PayloadT>(id, protocol, name, payload);
	}
	
	/**
	 * Gives the string representation of the message using a custom formatting
	 * for the message content.
	 * 
	 * @param formatting
	 *            a function returning Strings representing the message
	 *            contents.
	 * @return the String representing the message.
	 */
	public String toString(Function<PayloadT, String> formatting) {
		String prettyId = Base64.getUrlEncoder()
				.encodeToString(BigInteger.valueOf(id).toByteArray())
				.substring(0, 11);
		
		return String.format("%s: [%s %s:%s] %s", from, prettyId, protocol,
				name, payload == null ? "null" : formatting.apply(payload));
	}
	
	public String toString() {
		return toString(PayloadT::toString);
	}
}