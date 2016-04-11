package communications;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.function.Function;


public class ShortMessage<PayloadT> implements Serializable {
	
	private static final long serialVersionUID = 1666L;
	
	/**
	 * A buffer to be used in {@link #idToString(long)}.
	 */
	private static ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
	
	/**
	 * Utilities method to convert an id to a String. Use this when printing
	 * message id's.
	 * 
	 * @param id
	 *            a long representing the message id.
	 * @return a String representing the message id.
	 */
	public static String idToString(long id) {
		synchronized (byteBuffer) {
			byteBuffer.putLong(0, id);

			return Base64.getUrlEncoder().encodeToString(byteBuffer.array())
					.substring(0, 11);
		}
	}
	
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
	 * The content of the message.
	 */
	private final PayloadT payload;
	
	/**
	 * Builds a short message.
	 * 
	 * @param id
	 *            the id of the protocol round.
	 * @param protocol
	 *            the name of the protocol
	 * @param name
	 *            the name of the message.
	 * @param payload
	 *            the content of the message.
	 */
	public ShortMessage(long id, String protocol, String name, PayloadT payload) {
		super();
		this.id = id;
		this.protocol = protocol;
		this.name = name;
		this.payload = payload;
	}
	
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
	 * Returns a {@link FullMessage} object with the same information as the
	 * current message with "from" field given by the user.
	 * 
	 * @param from
	 *            a String representation of the source address of the message.
	 * @return a full message with the given source address.
	 * @see FullMessage
	 */
	public FullMessage<PayloadT> lengthen(String from) {
		return new FullMessage<PayloadT>(id, protocol, name, from, payload);
	}
	
	/**
	 * Prints message to a String, with a custom formatting for the payload. If
	 * the payload is to be printed with its own toString method, just use the
	 * default no-parameters toString method of this class.
	 * 
	 * @param formatting
	 *            a function to convert the payload to a meaningful String.
	 * @return a String representation of the message.
	 */
	public String toString(Function<PayloadT, String> formatting) {
		String prettyId = Base64.getUrlEncoder()
				.encodeToString(BigInteger.valueOf(id).toByteArray())
				.substring(0, 11);
		
		return String.format("[%s %s:%s] %s", prettyId, protocol, name,
				payload == null ? "null" : formatting.apply(payload));
	}
	
	public String toString() {
		return toString(PayloadT::toString);
	}
}
