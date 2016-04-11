/*package compose;

import java.text.ParseException;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.QueueResource;
import communications.ShortMessage;
import exceptions.TxException;


public class Connectionless<P, M>
		extends QueueResource<P> {
	
	private final String senderName, receiverName;
	private final SendingActor<M, ?> sender;
	private final ReceivingActor<M, ?> receiver;
	private final MessageEncodingScheme<P, M> scheme;
	
	
	public <Q> Connectionless(TransportPlay<M, Q> play,
			MessageEncodingScheme<P, M> scheme,
			CommunicationResource<Q> resource) {
		super();
		this.scheme = scheme;
		this.senderName = play.getSenderName();
		this.receiverName = play.getReceieverName();
		
		SendingActor<M, Q> sender = play.getSender();
		ReceivingActor<M, Q> receiver = play.getReceiver();
		
		sender.load(play.interpretAs(senderName), resource);
		receiver.load(play.interpretAs(receiverName), resource);
		
		this.sender = sender;
		this.receiver = receiver;
		
		start();
	}
	
	public void sendMessage(ShortMessage<P> msg, String to) {
		synchronized (sender) {
			try {
				sender.put(scheme.encode(msg));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (ParseException e) {
				throw new TxException(String.format(
						"Message encoding failed  for %s."
								+ " \n\tReason given: %s", msg, e.getMessage()));
			}
			
			sender.setInitialAddress(receiverName, to);
			sender.perform();
		}
	}
	
	public FullMessage<P> take() throws InterruptedException {
		M msg = null;
		
		synchronized (receiver) {
			receiver.perform();
			
			try {
				return scheme.decode(msg = receiver.take()).lengthen(
						receiver.getAddress(senderName));
			} catch (ParseException e) {
				throw new TxException(String.format(
						"Message decoding failed  for %s."
								+ " \n\tReason given: %s", msg, e.getMessage()));
			}
		}
	}
}*/