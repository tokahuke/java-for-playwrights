package communications.util;

import java.util.HashSet;
import java.util.Set;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.ReceiveEvent;


public abstract class QueueResource<PayloadT> implements
		CommunicationResource<PayloadT> {
	
	// Fields:
	
	/**
	 * Stores all events added to the resource. This was made a set to make
	 * removal easy.
	 */
	private final Set<ReceiveEvent<PayloadT>> events 
			= new HashSet<ReceiveEvent<PayloadT>>();
	
	/**
	 * The thread that listens to incoming messages.
	 */
	private Thread listener = null;
	
	
	// Abstract methods:
	
	/**
	 * Receives all messages arriving from the network.
	 * 
	 * @return the received message.
	 * @throws InterruptedException
	 *             when the Thread is interrupted by {@link #stop()} or any
	 *             other external cause.
	 */
	public abstract FullMessage<PayloadT> take() throws InterruptedException;
	
	private void listenMessages() {
		try {
			while (true) {
				FullMessage<PayloadT> msg = take();

				synchronized (events) {
					for (ReceiveEvent<PayloadT> receiveEvent : events) {
						if (receiveEvent.receives(msg)) {
							break;
						}
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	// TODO is this the best pattern? Or should I pass this obligation to the
	// poor users (and also the privilege of finer control)?
	
	/**
	 * Starts the listener Thread. Should <em>always</em> be invoked at the end
	 * of any subclass constructor.
	 */
	protected void start() {
		listener = new Thread(this::listenMessages);
		listener.start();
	}
	
	/**
	 * Interrupts the listener thread.
	 */
	protected void stop() {
		listener.interrupt();
	}
	
	
	// Implementation of the CommunicationResource interface:
	
	@Override public void addReceiveEvent(ReceiveEvent<PayloadT> receiveEvent) {
		synchronized (events) {
			events.add(receiveEvent);
		}
	}
	
	@Override public void removeReceiveEvent(ReceiveEvent<PayloadT> receiveEvent) {
		synchronized (events) {
			events.remove(receiveEvent);
		}
	}
}
