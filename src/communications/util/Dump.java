package communications.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import communications.CommunicationResource;
import communications.ReceiveEvent;
import communications.TxException;
import communications.ShortMessage;

public class Dump<P> implements CommunicationResource<P> {
	
	/**
	 * Enumerates the type of traffic to be shown on the screen (Whether
	 * send-side only, receive-side only or both sides).
	 * 
	 * @author rongomai
	 *
	 */
	public static enum Show {
		SEND, RECEIVE, BOTH_WAYS;
	}
	
	/*
	 * The resource to be used to actually send the messages.
	 */
	private final CommunicationResource<P> resource;
	
	/**
	 * A mapping between the receive events given from the environment and the
	 * ones installed in the communication resource given.
	 */
	private final Map<ReceiveEvent<P>, ReceiveEvent<P>> modificationMap;
	
	/**
	 * The formatting style to be used for the payload.
	 */
	private final Function<P, String> formatting;
	
	/**
	 * The type of traffic to be shown by this object.
	 */
	private final Show show;
	
	/**
	 * A String representation of the this host's address.
	 */
	private final String localAddress;
	
	
	// Constructors:
	
	/**
	 * Creates a Dump object over a given resource. This object shows both
	 * incoming and outgoing traffic and uses the payload's own toString method
	 * as formatting style.
	 * 
	 * @param resource
	 *            the communication resource to be monitored.
	 */
	public Dump(CommunicationResource<P> resource) {
		this(resource, p -> p == null ? "null" : p.toString(), Show.BOTH_WAYS);
	}
	
	/**
	 * Creates a Dump object over a given resource showing the traffic as
	 * specified by the user. This object uses the payload's own toString method
	 * as formatting style.
	 * 
	 * @param resource
	 *            the communication resource to be monitored.
	 * @param show
	 *            a {@link Show} indicating what type of traffic will be shown:
	 *            whether incoming, outgoing or both.
	 */
	public Dump(CommunicationResource<P> resource, Show show) {
		this(resource, p -> p == null ? "null" : p.toString(), show);
	}
	
	/**
	 * Creates a Dump object over a given resource using a formatting for the
	 * payload specified by the user. This object shows both incoming and
	 * outgoing traffic.
	 * 
	 * @param resource
	 *            the communication resource to be monitored.
	 * @param formatting
	 *            the formatting to be used to display the payload on screen. It
	 *            must be able to handle the case where the payload is null.
	 */
	public Dump(CommunicationResource<P> resource,
			Function<P, String> formatting) {
		this(resource, p -> p == null ? "null" : p.toString(), Show.BOTH_WAYS);
	}
	
	/**
	 *  Creates a Dump object over a given resource using a formatting for the
	 * payload specified by the user
	 * 
	 * @param resource
	 *            the communication resource to be monitored.
	 * @param formatting
	 *            the formatting to be used to display the payload on screen. It
	 *            must be able to handle the case where the payload is null.
	 *  @param show
	 *            a {@link Show} indicating what type of traffic will be shown:
	 *            whether incoming, outgoing or both.
	 */
	public Dump(CommunicationResource<P> resource,
			Function<P, String> formatting, Show show) {
		this.resource = resource;
		this.formatting = formatting;
		this.show = show;
		this.modificationMap = new HashMap<ReceiveEvent<P>, ReceiveEvent<P>>();
		this.localAddress = resource.getLocalAddress();
	}
	
	
	// Implementation of the CommuncationResource interface:
	
	@Override public void addReceiveEvent(ReceiveEvent<P> receiveEvent) {
		if (show == Show.RECEIVE || show == Show.BOTH_WAYS) {
			ReceiveEvent<P> modified = msg -> {
				if (receiveEvent.receives(msg)) {
					if (localAddress != null) {
						System.out.printf("-- from %s to %s: %s\n", msg
								.getFrom(), localAddress, msg.shorten()
								.toString(formatting));
					} else {
						System.out.printf("-- from %s: %s\n", msg.getFrom(),
								msg.shorten().toString(formatting));
					}
					return true;
				} else {
					return false;
				}
			};
			
			modificationMap.put(receiveEvent, modified);
			
			resource.addReceiveEvent(modified);
		} else {
			resource.addReceiveEvent(receiveEvent);
		}
	}

	@Override public void removeReceiveEvent(ReceiveEvent<P> receiveEvent) {
		resource.removeReceiveEvent(modificationMap.get(receiveEvent));
	}

	@Override public void sendMessage(ShortMessage<P> msg, String to)
			throws TxException {
		if (show == Show.SEND || show == Show.BOTH_WAYS) {
			if (localAddress != null) {
				System.out.printf("-- from %s to %s: %s\n", localAddress, to,
						msg.toString(formatting));
			} else {
				System.out.printf("-- to   %s: %s\n", to,
					msg.toString(formatting));
			}
		}
		
		resource.sendMessage(msg, to);
	}
}
