package compose;

import java.util.concurrent.BlockingQueue;

import dsl.Actor;

public class ReceivingActor<M, P> extends Actor<P> {
	
	private BlockingQueue<MessageSenderPair<M>> returnQueue;
	private String senderName;
	
	/* package-private */ void setQueueAndSenderName(
			BlockingQueue<MessageSenderPair<M>> queue,
			String senderName) {
		this.returnQueue = queue;
		this.senderName = senderName;
	}
	
	public void put(M msg) {
		try {
			// Find out who the message is from:
			String senderAddress = getAddress(senderName);
			
			// In case connection is anonymous:
			if (senderAddress == null) {
				senderAddress = String.format("!%s_%x", senderName, getRunId());
			}
			
			// Put in queue:
			returnQueue.put(new MessageSenderPair<M>(msg,
					senderAddress));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	/* package-private */ MessageSenderPair<M> take()
			throws InterruptedException {
		return returnQueue.take();
	}
}