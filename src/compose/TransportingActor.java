package compose;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dsl.Actor;


public class TransportingActor<M, P> extends Actor<P> {
	
	private BlockingQueue<TransportingActor<M, P>> returnQueue;
	private BlockingQueue<M> payloadQueue = new LinkedBlockingQueue<M>();
	
	/* package-private */ void setReturnQueue(
			BlockingQueue<TransportingActor<M, P>> queue) {
		this.returnQueue = queue;
	}
	
	public void put(M msg) throws InterruptedException {
		payloadQueue.put(msg);
		returnQueue.put(this);
	}
	
	public M take() throws InterruptedException {
		return payloadQueue.take();
	}
}