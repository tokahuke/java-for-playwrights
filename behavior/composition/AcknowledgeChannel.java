package composition;

import compose.ReceivingActor;
import compose.SendingActor;
import compose.TransportPlay;

public class AcknowledgeChannel<P> extends TransportPlay<P, P> {
	
	public static String SENDER = "sender";
	public static String RECEIVER = "receiver";
	
	private Character<SendingActor<P, P>> sender;
	private Character<ReceivingActor<P, P>> receiver;
	
	public AcknowledgeChannel() {
		super.protocolName = "ackCh";
	}
	
	@Override public void dramatisPersonae() {
		sender = new Character<SendingActor<P, P>>(
				SendingActor.class, SENDER);
		receiver = new Character<ReceivingActor<P, P>>(
				ReceivingActor.class, RECEIVER);
	}
	
	@Override public void scene() {
		sender.send(receiver, SendingActor::take, ReceivingActor::put, "msg", 5);
		receiver.send(sender, "ack", 5);
	}

	@Override public String getSenderName() {
		return SENDER;
	}

	@Override public String getReceieverName() {
		return RECEIVER;
	}

	@Override public SendingActor<P, P> getSender() {
		return new SendingActor<P, P>();
	}

	@Override public ReceivingActor<P, P> getReceiver() {
		return new ReceivingActor<P, P>();
	}
}