package dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.ReceiveEvent;


public class Server<P> {
	
	// TODO Re-implement using Java's Executors instead. You get something
	// better and for less lines of code.
	
	// Internal classes:
	
	/**
	 * Enumerates the possible states of the Server, that is, whether it is
	 * running, stopped or aborted (abruptly stopped).
	 * 
	 * @author rongomai
	 *
	 */
	private static enum RunStatus {
		RUNNING, STOPPED, ABORTED
	};
	
	
	// Fields:
	
	/**
	 * Current state of the server.
	 * 
	 * @see Server.RunStatus
	 */
	private RunStatus status = RunStatus.ABORTED;
	
	/**
	 * The part to be interpreted by this Server object.
	 */
	private final Part part;
	
	/**
	 * The queue of actors available to perform a protocol round.
	 */
	private final BlockingQueue<Integer> actorIdQueue;
	
	/**
	 * The pool of threads in which actors are running (one Thread, one Actor).
	 */
	private final Thread[] threadPool;
	
	/**
	 * An array pooling all created Actor objects.
	 */
	private final Actor<P>[] actorPool;
	
	/**
	 * The list of addresses for characters that is fixed for all runs of the
	 * protocol.
	 */
	private final Map<String, String> initialAddressMap = 
			new HashMap<String, String>();
	
	/**
	 * The communications protocol implementation to be used.
	 */
	private final CommunicationResource<P> communicationResource;
	
	/**
	 * The map of run IDs to the objects performing the run.
	 */
	private final Map<Long, Actor<P>> ongoingRuns = 
			new HashMap<Long, Actor<P>>();
	
	
	// Constructor:
	
	/**
	 * Creates a new Server object to interpret the protocol Part given using a
	 * certain CommunicationsResource.
	 * 
	 * @param <A>
	 *            the Actor type to be used by this Server.
	 * @param part
	 *            the Part to be interpreted by the Server.
	 * @param communicationResource
	 *            the resource to be used to send and receive messages.
	 * @param actorFactory
	 *            a factory to instantiate new Actors (and do any necessary
	 *            setup before running).
	 * @param size
	 *            the maximum number of actors that can run simultaneously.
	 */
	@SuppressWarnings("unchecked") public <A extends Actor<P>> Server(
			Part part, CommunicationResource<P> communicationResource,
			Supplier<A> actorFactory, int size) {
		this.part = part;
		this.communicationResource = communicationResource;
		
		actorIdQueue = new ArrayBlockingQueue<Integer>(size);
		threadPool = new Thread[size];
		actorPool = (A[]) new Actor[size];
		
		for (int i = 0; i < size; i++) {
			A actor = actorFactory.get();
			actor.load(part, communicationResource, initialAddressMap);
			actorPool[i] = actor;
			threadPool[i] = buildThread(i, actor);
		}
	}
	
	/**
	 * Creates a new Thread object to run an Actor.
	 * 
	 * @param id
	 *            the id of the Actor.
	 * @param actor
	 *            the Actor object to be run by the thread.
	 * @return an inactive Thread object (invoke {@link Thread#start()} to start
	 *         the thread).
	 */
	private Thread buildThread(int id, Actor<?> actor) {
		return new Thread(new Runnable() {
			public void run() {
				while (true) {
					// Go to the end of the line!
					try {
						actorIdQueue.put(id);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					
					// Run it!
					actor.perform();
					
					// End:
					ongoingRuns.remove(actor.getRunId());
				}
			}
		}, actor.getCharacter() + "_" + id);
	}
	
	/**
	 * This method is an event handler to identify and process new runs of the
	 * given protocol that arrive through the communication resource given.
	 * 
	 * @param inMessage
	 *            an incoming message.
	 * @return true if the message belongs to a valid new run; false otherwise.
	 * @see ReceiveEvent
	 */
	private boolean freshRunEvent(FullMessage<P> inMessage) {
		if (!ongoingRuns.containsKey(inMessage.getId())
				&& part.inMessageIds.containsKey(inMessage.getName())) {
			long runId = inMessage.getId();
			
			try {
				Actor<P> actor = actorPool[actorIdQueue.take()];
				actor.setRunId(runId);
				ongoingRuns.put(runId, actor);
				actor.getMessageQueue().put(inMessage);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			return true;
		} else
			return false;
	}
	
	/**
	 * Starts the Server.
	 */
	public void start() {
		if (status != RunStatus.RUNNING) {
			if (status == RunStatus.ABORTED) {
				for (Thread thread : threadPool) {
					thread.start();
				}
			}
			
			status = RunStatus.RUNNING;
			
			communicationResource.addReceiveEvent(this::freshRunEvent);
		}
	}
	
	/**
	 * Stops the server. This only prevents the server from accepting new rounds
	 * and does not affect the ones already running.
	 */
	public void stop() {
		if (status != RunStatus.STOPPED) {
			if (status != RunStatus.ABORTED) {
				communicationResource.removeReceiveEvent(this::freshRunEvent);
				status = RunStatus.STOPPED;
			}
		}
	}
	
	/**
	 * Aborts the server. Existing rounds will be forced to stop by interrupting
	 * all running threads. No new rounds will be accepted.
	 */
	public void abort() {
		if (status != RunStatus.ABORTED) {
			communicationResource.removeReceiveEvent(this::freshRunEvent);
			
			for (Thread thread : threadPool) {
				thread.interrupt();
			}
			
			ongoingRuns.clear();
			
			status = RunStatus.ABORTED;
		}
	}
	
	/**
	 * The filter that selects all messages pertaining to an active runs. This
	 * method is used as a Predicate to decide whether the associated message
	 * receive event will be executed or not.
	 * 
	 * @param inMessage
	 *            the received message to be tested.
	 * @return true in case the message pertains to an active run.
	 * @see #ongoingRunMRE(Message<PayloadT>) ongoingRunMRE
	 */
/*	public boolean ongoingRunEvent(FullMessage<PayloadT> inMessage) {
		if (ongoingRuns.keySet().contains(inMessage.getId())) {
			try {
				ongoingRuns.get(inMessage.getId()).getMessageQueue()
						.put(inMessage);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			return true; // Nope! It is not that stupid here.
		} else {
			return false;
		}
	}*/
	
	/**
	 * Sets the address of a character in the {@link #initialAddressMap}. This
	 * address is the one (and the only one) that will be used by all protocol
	 * runs. The idea here is to either restrict the addresses that can perform
	 * a certain character or to set addresses that have to be known beforehand
	 * (e.g. a server address).
	 * 
	 * @param identifier
	 *            the character whose address is being set
	 * @param address
	 *            the address to be set for the character
	 */
	public void setInitialAddress(String identifier, String address) {
		initialAddressMap.put(identifier, address);
	}
	
	/**
	 * Gets the address for the given character in the
	 * {@link #initialAddressMap}.
	 * 
	 * @param identifier
	 *            the character for which the address is to be given.
	 * @return the address set for the character or null, in case none is set.
	 */
	public String getInitialAddress(String identifier) {
		return initialAddressMap.get(identifier);
	}
}