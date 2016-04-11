package protocols;

import org.jivesoftware.smack.packet.IQ;

import communications.FullMessage;

public class MyIQ extends IQ {
	public static String protocol = "xmpp";
	
	private final String type;
	private final String payload;

	MyIQ(String type, String payload) {
		this.type = type;
		this.payload = payload;
	}
	
	public FullMessage<String> toMessage() {
		return new FullMessage<String>(Long.valueOf(getPacketID()), protocol,
				type, getFrom(), payload);
	}

	@Override public String getChildElementXML() {
		// The type is encoded as an XML tag and the content as its content:
		return  String.format("<query xmlns='iq:myOwn'><%s>%s</%s></query>", type,
				payload, type);
	}
}