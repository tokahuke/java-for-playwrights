package protocols;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import communications.FullMessage;
import communications.ShortMessage;
import communications.TxException;
import communications.util.QueueResource;


public class ThreadProtocol<P> extends QueueResource<P> {
	
	// Fields:
	
	/**
	 * The queue for incoming messages.
	 */
	private final BlockingQueue<FullMessage<P>> blockingQueue;
	
	/**
	 * The map between addresses and queues. This simulates the network.
	 */
	private final Map<String, BlockingQueue<FullMessage<P>>> blockingQueues;
	
	/**
	 * The address of the host.
	 */
	private final String me;
	
	/**
	 * Number of messages sent on this interface.
	 */
	private long numberOfTxMessages = 0;
	
	/**
	 * Number of messages received on this interface.
	 */
	private long numberOfRxMessages = 0;
	
	// Constructors:

	/**
	 * Creates a new ThreadProtocol resource for a certain address, given as any
	 * String, and a channel simulated by a BlockingQueue map.
	 * 
	 * @param blockingQueues
	 *            the mapping between addresses and BlockingQueue "inboxes".
	 * @param me
	 *            the address to which the object created responds to (i.e., the
	 *            local address). It can be any String.
	 */
	public ThreadProtocol(
			Map<String, BlockingQueue<FullMessage<P>>> blockingQueues,
			String me) {
		super();
		this.blockingQueue = new LinkedBlockingQueue<FullMessage<P>>();
		this.blockingQueues = blockingQueues;
		this.me = me;
		blockingQueues.put(me, blockingQueue);
		
		super.start();
	}
	
	/**
	 * Creates a new ThreadProtocol resource for a channel simulated by a
	 * BlockingQueue map. The local address is set to the name of the Thread in
	 * which the object was instantiated.
	 * 
	 * @param blockingQueues
	 *            the mapping between addresses and BlockingQueue "inboxes".
	 */
	public ThreadProtocol(
			Map<String, BlockingQueue<FullMessage<P>>> blockingQueues) {
		this(blockingQueues, Thread.currentThread().getName());
	}
	
	
	// Implementation of the QueueResource interface:
	
	@Override public void sendMessage(ShortMessage<P> msg, String to) {
		try {
			FullMessage<P> fullMsg = msg.lengthen(me);
			
//			if (new Random().nextFloat() < 0.01) {
//				return;
//			}
			
			numberOfTxMessages++;
			
			try {
				blockingQueues.get(to).put(fullMsg);
			} catch (NullPointerException e) {
				// Panic on non existent addresses:
				throw new TxException();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	@Override public FullMessage<P> take() throws InterruptedException {
		FullMessage<P> msg = blockingQueue.take();
		numberOfRxMessages++;
		return msg;
	}
	
	@Override public String getLocalAddress() {
		return me;
	}
	
	public long getNumberOfRxMessages() {
		return numberOfRxMessages;
	}
	
	public long getNumberOfTxMessages() {
		return numberOfTxMessages;
	}
}