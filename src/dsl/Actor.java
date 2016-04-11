package dsl;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.NoSuchCharacter;
import communications.NoSuchMessage;
import communications.RxException;
import communications.ShortMessage;
import communications.TimeoutException;


/**
 * 
 * This class executes a protocol description as one of its participants, using
 * a CommunicationResource to access the network. An Actor can be initialized
 * without specifying any protocol or resource, which can be useful when
 * extending the class or writing factories. However, in order to the Actor to
 * be able to {@link #perform() perform}, it must eventually be loaded with a
 * {@link Part} to interpret and a {@link CommunicationResource} to access the
 * network. Unloaded actors throw an exception when requested to
 * {@link #perform() perform}. On the other side, Actors cannot change the part
 * they interpret nor the resource they use once they are loaded (i.e., they are
 * immutable in that respect). This enhances the object's thread safety.
 * 
 * <p>
 * Since Actors are sequential units in nature, they only need one thread to
 * run. Do not use the same Actor in different threads! This might lead to
 * unexpected behavior. Also, if the part to be interpreted starts by receiving
 * a message (i.e., it corresponds to a passive role), using {@link #perform()}
 * directly may cause the program to lock for a long period of time, even
 * indefinitely. In this case, consider using {@link Server}, an Actor pool,
 * instead.
 * 
 * @author rongomai
 *
 * @param <P>
 *            the type of the payload this Actor works with.
 * @see Server
 * @see Part
 */
public class Actor<P> {
	
	// Exceptions:
	
	/**
	 * An exception raised when the {@link Actor} is requested to
	 * {@link Actor#perform()} but it does not have any {@link Part} to
	 * interpret.
	 * 
	 * @author rongomai
	 *
	 */
	public static class NotLoadedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;
		
		public NotLoadedException(String string) {
			super(string);
		}
	}
	
	/**
	 * An exception raised when the {@link Actor} is asked to load another
	 * {@link Part}. Actors are immutable in this respect.
	 * 
	 * @author rongomai
	 *
	 */
	public static class AlreadyLoadedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;
	}
	
	// Static stuff:
	
	/**
	 * A secure random number generator object to generate protocol round
	 * identifiers.
	 */
	private static final Random random = new SecureRandom();
	
	
	// These come from the environment:
	
	/**
	 * The part this Actor interprets.
	 */
	private Part part;
	
	/**
	 * The current protocol round identifier.
	 */
	private Long runId;
	
	/**
	 * The underlying protocol this actor uses to communicate.
	 */
	private CommunicationResource<P> communicationResource;
	
	/**
	 * The mapping between addresses and roles to be used at the beginning of
	 * each round.
	 * 
	 * @see #addressMap
	 */
	private Map<String, String> initialAddressMap;
	
	
	// And this is the internal state of the beast:
	
	/**
	 * The incoming message queue.
	 */
	private final BlockingQueue<FullMessage<P>> queue =
			new LinkedBlockingQueue<FullMessage<P>>();
	
	/**
	 * The content of each incoming message, ordered by incoming message id.
	 */
	private P[] inMessages;
	
	/**
	 * The content of each outgoing message, ordered by outgoing message id.
	 */
	private P[] outMessages;
	
	/**
	 * The current address mapping ordered by character id. This is set
	 * initially to the content of {@link #initialAddressMap} and may be altered
	 * along the run.
	 * 
	 * @see #initialAddressMap
	 */
	private String[] addressMap;
	
	/**
	 * A mapping of yet unconfirmed addresses ordered by character id. After a
	 * message is marked as accepted (i.e, non spurious), the address is moved
	 * away from this buffer.
	 * 
	 * @see #addressMap
	 */
	private String[] addressBuffer;
	
	/**
	 * A stack to search the incoming message graph.
	 */
	private short[] searchStack;
	
	/**
	 * A parent set to build the search tree of the incoming message graph.
	 */
	private short[] parent;
	
	/**
	 * A set marking explored nodes in the incoming message graph.
	 */
	private BitSet explored;
	
	/**
	 * A set marking accepted (i.e., non spurious) incoming messages.
	 */
	private BitSet accepted;
	
	/**
	 * A set marking messages already sent.
	 */
	private BitSet sent;
	
	/**
	 * Stores the maximum arrival time for each incoming message. A position is
	 * set to -1 if the corresponding message is not expected to arrive due to
	 * causality constraints.
	 */
	private int[] maxTimes;
	
	/**
	 * Stores the time the current run started.
	 */
	private long beginTime;
	
	/**
	 * A priority queue to store incoming messages according to their expiration
	 * date.
	 */
	private PriorityQueue<Short> expiryQueue;
	
	
	// Constructors and building methods:
	
	/**
	 * Builds a new Actor object, but does not initialize it.
	 * 
	 * @see #Actor(Part, CommunicationResource)
	 * @see #load(Part, CommunicationResource)
	 */
	public Actor() {
		
	}
	
	/**
	 * Builds a new Actor object and initializes it to interpret a {@link Part}
	 * over a {@link CommunicationResource}.
	 * 
	 * @param part
	 *            the {@link Part} to be interpreted.
	 * @param communicationResource
	 *            the {@link CommunicationResource} to be used as the means of
	 *            exchanging messages.
	 */
	public Actor(Part part, CommunicationResource<P> communicationResource) {
		this();
		load(part, communicationResource);
	}
	
	/**
	 * Initializes an yet uninitialized Actor whith an empty address map.
	 * 
	 * @param part
	 *            the {@link Part} to be interpreted.
	 * @param communicationResource
	 *            the {@link CommunicationResource} to be used as the means of
	 *            exchanging messages.
	 * @throws AlreadyLoadedException
	 *             in case the object has already been loaded.
	 * @see #load(Part, CommunicationResource, Map)
	 */
	public void load(Part part, CommunicationResource<P> communicationResource) {
		load(part, communicationResource, new HashMap<String, String>());
	}
	
	/**
	 * Initializes an yet uninitialized Actor.
	 * 
	 * @param part
	 *            the {@link Part} to be interpreted.
	 * @param communicationResource
	 *            the {@link CommunicationResource} to be used as the means of
	 *            exchanging messages.
	 * @param initialAddressMap
	 *            the initial address map to be loaded at every run.
	 * @throws AlreadyLoadedException
	 *             in case the object has already been loaded.
	 */
	@SuppressWarnings("unchecked") public void load(Part part,
			CommunicationResource<P> communicationResource,
			Map<String, String> initialAddressMap) {
		
		// Do not allow overriding:
		if (part == null) {
			throw new AlreadyLoadedException();
		}
		
		// Load part:
		this.part = part;
		
		// Now check if the classes match!
		if (!part.actorClass.isInstance(this)) {
			throw new IllegalStateException(String.format(
					"The part given is for %s; this actor has class %s.",
					part.actorClass.getCanonicalName(), this.getClass()
							.getCanonicalName()));
		}
		
		// Useful debug:
//		System.out.println(part);
		
		// Initialize other variables:
		int inMessageNumber = part.inMessageIds.size();
		int outMessageNumber = part.outMessageIds.size();
		int characterNumber = part.characterIds.size();
		
		this.communicationResource = communicationResource;
		this.initialAddressMap = initialAddressMap;
		
		this.inMessages = (P[]) new Object[inMessageNumber];
		this.outMessages = (P[]) new Object[outMessageNumber];
		this.addressMap = new String[characterNumber];
		this.addressBuffer = new String[inMessageNumber];
		this.searchStack = new short[part.stackSize];
		this.parent = new short[inMessageNumber];
		this.explored = new BitSet(inMessageNumber);
		this.accepted = new BitSet(inMessageNumber);
		this.sent = new BitSet(outMessageNumber);
		this.maxTimes = new int[inMessageNumber];
		this.expiryQueue = new PriorityQueue<Short>(
				Comparator.comparingInt(msgId -> maxTimes[msgId]));
	}
	
	
	// The mechanism of the thing:
	
	/**
	 * Provides a reference to the message queue.
	 * 
	 * @return the message queue used to store incoming messages.
	 */
	public BlockingQueue<FullMessage<P>> getMessageQueue() {
		return queue;
	}
	
	/**
	 * Performs a round of the protocol as described in the {@link Part} object
	 * supplied.
	 * 
	 * @throws NotLoadedException
	 *             in case the object has not yet been initialized.
	 * @see #load(Part, CommunicationResource, Map)
	 */
	public void perform() {
		reset();
		performWithoutResetting();
	}
	
	/**
	 * Performs a round of the protocol as described in the {@link Part} object
	 * supplied, but does not resets the object to its initial state in the end.
	 * Use this only if you are shore of what you are doing. Prefer using
	 * {@link #perform()} instead.
	 * 
	 * @throws NotLoadedException
	 *             in case the object has not yet been initialized.
	 * @see #load(Part, CommunicationResource, Map)
	 * @see #perform()
	 * @see #reset()
	 */
	public void performWithoutResetting() {
		if (part == null) {
			throw new NotLoadedException("Please invoke Actor#load before use.");
		}
		
		BitSet initials = new BitSet();
		
		// Oh! What time is it?
		beginTime = Clock.systemUTC().millis();
		
		// Start listening:
		communicationResource.addReceiveEvent(this::ongoingRunEvent);
		
		// Zhu Lee! Do the thing!
		try {
			for (Node node = part.rootNode; !(node instanceof EndNode); /* */) {
				// Useful debug:
//				System.out.println(part.characterName + " : " + node);
				
				// Get messages that might arrive from NoReceive handlers:
				if (node instanceof SendNode
						&& ((SendNode) node).getHandlerMessages() != null) {
					initials.or(((SendNode) node).getHandlerMessages());
				}
				
				// Execute:
				Node next = node.next(this);
				Object nextAction = next.getNextAction();
				
				// Decide what comes next:
				if (nextAction != null) {
					if (nextAction instanceof Node) {
						node = (Node) nextAction;
					} else {
						initials.or((BitSet) nextAction);
						node = receiveMessages(initials);
					}
				} else {
					node = next;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			reset();
		} finally {
			// Useful debug:
//			System.out.println(Thread.currentThread().getName());
			
			// Stop listening:
			communicationResource.removeReceiveEvent(this::ongoingRunEvent);
		}
	}
	
	/**
	 * Receives messages until a path of confirmed (i.e., possibly not spurious)
	 * has been found from one of the messages marked as initial in the BitSet
	 * provided to an active Node.
	 * 
	 * @param initials
	 *            the BitSet of initial incoming messages to be received.
	 * @return the active Node found (this is the next action in the execution
	 *         path).
	 * @throws InterruptedException it the current thread is interrupted.
	 */
	private Node receiveMessages(BitSet initials) throws InterruptedException {
		FullMessage<P> message;
		Node nextNode = null;
		BitSet connectivityProof = getConnectivityProof(initials);
		
		do {
			// Receive a message or lose your temper:
			if (expiryQueue.isEmpty()) {
				// Relax and just wait until something gets here:
				// System.out.println("MaxT:" + Arrays.toString(maxTimes));
				// System.out.println("ExpQ:" + expiryQueue);
				// System.out.println("CC:" +Arrays.toString(causalCount));
				message = queue.take();
			} else {
				// Wait until all patience is gone:
				message = queue.poll(maxTimes[expiryQueue.peek()]
						- Clock.systemUTC().millis() + beginTime,
						TimeUnit.MILLISECONDS);
				
				// If a message expires:
				if (message == null) {
					// Remove from priority queue:
					int expiredMessageId = expiryQueue.remove();
					
					// Mark message as expired:
					maxTimes[expiredMessageId] = -1;
					
					// Check if connectivity proof still holds:
					if (!connectivityProof.get(expiredMessageId)) {
						// If it does hold, no problem! Continue waiting:
						continue;
					} else {
						// Nope, it doesn't! Calculate new connectivity proof:
						connectivityProof = getConnectivityProof(initials);
						
						// In case the graph is disconnected:
						if (connectivityProof.isEmpty()) {
							if (part.noReceiveHandlers[expiredMessageId] instanceof EndNode) {
								// TODO get name from message id (argh, Java!)
								System.out.println(runId);
								throw new TimeoutException(part.protocolName,
										"X", runId);
							} else {
								return part.noReceiveHandlers[expiredMessageId];
							}
						}
					}
				}
			}
			
			// Now that we have something, lets check if is legit and what it
			// means:
			//System.out.println(part.characterName + " got " + message);
			
			short messageId = part.inMessageIds.get(message.getName());
			String inferredAddress = addressMap[part.characterForMessage[messageId]];
			
			// Useful debug:
//			System.out.println(!explored.get(messageId));
//			System.out.println(maxTimes[messageId]);
//			System.out.println((inferredAddress == null || inferredAddress
//			.equals(message.getFrom())));
			
			// Conditions to consider a message as non spurious: it is not
			// already explored, but it must also
			// have been completely caused and the inferred address must be
			// equal to the one given in the map (or there must be none given).
			// Additionally, it may not have already expired.
			if (!explored.get(messageId)
					&& maxTimes[messageId] >= 0
					&& (inferredAddress == null || inferredAddress
							.equals(message.getFrom()))) {
				// Put in buffer:
				inMessages[messageId] = message.getPayload();
				addressBuffer[messageId] = message.getFrom();
				
				// Check if message is initial:
				if (initials.get(messageId)) {
					// Initial are trivially explored:
					if ((nextNode = searchForFinal(messageId)) != null) {
						break; // No need to carry on...
					}
				} else {
					// Check if message connects to previously explored:
					for (short previous : part.nextMessagesReverse[messageId]) {
						if (explored.get(previous)) {
							// If connects, do search for a final message:
							parent[messageId] = (short) (previous + 1);
							nextNode = searchForFinal(messageId);
							break;
						}
					}
				}
			}
		} while (nextNode == null);
		
		// Learn search id:
		if (runId == null) {
			runId = message.getId();
		}
		
		explored.clear(); // Remember to clear search variables!
		
		return nextNode;
	}
	
	/**
	 * Establishes whether any messages can still be received. This method looks
	 * for a path from any of the initial messages provided and tries to find a
	 * path of receivable messages to a Node. This path, if non-empty, serves as
	 * proof that it is still reasonable to wait for new messages to arrive
	 * (instead of, say, raising a {@link RxException}).
	 * 
	 * @param initials
	 *            a BitSet with the positions to start the search from set.
	 * @return a BitSet with the positions in the proof path set.
	 */
	private BitSet getConnectivityProof(BitSet initials) {
		
		// Initializing search variables:
		BitSet proof = new BitSet();
		short[] stack = this.searchStack; // Using existing stack.
		short stackPointer = 0;
		BitSet explored = new BitSet();
		short[] parent = new short[inMessages.length];
		
		// Put initials in stack:
		for (short i = 0; i < initials.length(); i++) {
			if (initials.get(i) && maxTimes[i] >= 0) {
				stack[stackPointer++] = i;
			}
		}
		
		// Depth first search:
		while (stackPointer != 0) {
			// Dequeue:
			short current = stack[--stackPointer];
			explored.set(current);
			
			// Analyze:
			if (part.nextActions[current] instanceof Node) {
				// Found a path!
				for (short id = current; id != -1; id = (short) (parent[id] - 1)) {
					proof.set(id);
				}
				
				// Search done!
				break;
			} else {
				// Put valid adjacencies in queue:
				for (short next : (short[]) part.nextActions[current]) {
					// Only put those messages that may arrive:
					if (!explored.get(next) && maxTimes[next] >= 0) {
						stack[stackPointer++] = next;
						parent[next] = (short) (current + 1);
					}
				}
			}
		}
		
		return proof;
	}
	
	/**
	 * Does a search for a final message in the incoming message graph that is
	 * connected to the message id provided. The connection has to be done
	 * through messages that arrived but were not confirmed yet.
	 * 
	 * @param messageId
	 *            the id of the beginning of the search.
	 * @return the next action in the execution path if it was found; null
	 *         otherwise.
	 */
	private Node searchForFinal(short messageId) {
		
		// Search variables initialization:
		short[] stack = searchStack;
		stack[0] = messageId;
		short stackPointer = 1;
		boolean foundFinal = false;
		
		// Depth first search:
		while (stackPointer != 0) {
			// Dequeue:
			short current = stack[--stackPointer];
			explored.set(current);
			
			// Analyze:
			if (part.nextActions[current] instanceof Node) {
				// Found a final message
				foundFinal = true;
				break;
			} else {
				for (short next : (short[]) part.nextActions[current]) {
					// Only put those messages that arrived:
					if (inMessages[next] != null) {
						stack[stackPointer++] = next;
						parent[next] = (short) (current + 1);
					}
				}
			}
		}
		
		// If found a final message, backtrack and learn:
		if (foundFinal) {
			// TODO Actually this piece of code is conceptually wrong. See my
			// notes on that for further detail. Instead, we have to implement a
			// longest (critical) path algorithm and accept the messages in this
			// path.
			
			short finalId = stack[stackPointer];
			
			for (short id = finalId; id != -1; id = (short) (parent[id] - 1)) {
				accepted.set(id);
				addressMap[part.characterForMessage[id]] = addressBuffer[id];
			}
			
			// And it's done. Next state!
			return (Node) part.nextActions[finalId];
		}
		
		// Else, do nothing.
		return null;
	}
	
	/**
	 * Resets the actor back to it's initial state (addresses set to the ones of
	 * the initial map, no messages sent, no messages received).
	 */
	public void reset() {
		
		// Clear address dictionary:
		for (String character : part.characterIds.keySet()) {
			addressMap[part.characterIds.get(character)] = initialAddressMap
					.get(character);
		}
		
		// Clear maximum times:
		short msgNum = (short) (part.inMessageIds.size() - 1);
		for (short in = 0; in < msgNum; in++) {
			maxTimes[in] = part.isCausFinal[in].isEmpty()
					|| part.isSpontaneous.get(in) ? Integer.MAX_VALUE : -1;
		}
		
		// Empty queue:
		expiryQueue.clear();
		
		// Clear buffer:
		accepted.clear();
		
		// Clear run id:
		runId = null;
	}
	
	/**
	 * Sends a message.
	 * 
	 * @param receiver
	 *            the character identifier of the receiver.
	 * @param name
	 *            the name of the message.
	 */
	/* package-private */void sendMessage(String receiver, String name) {
		// Resolve name into an id:
		short outMessageId = part.outMessageIds.get(name);
		
		// Invent a run id if necessary:
		if (runId == null) {
			runId = random.nextLong();
		}
		
		// Send the thing:
		communicationResource.sendMessage(new ShortMessage<P>(runId, // id
				part.protocolName,			  // protocol name
				name, 						  // message name
				outMessages[outMessageId]),   // payload
				addressMap[part.characterIds.get(receiver)]); // receiver
		
		// Set the maximum tolerances for incoming messages:
		int now = (int) (Clock.systemUTC().millis() - beginTime);
		short maxCausalId = (short) part.causalRelation[outMessageId].length;
		
		for (short causalId = 0; causalId < maxCausalId; causalId++) {
			short inMessageId = part.causalRelation[outMessageId][causalId];
			int newMaxTime = now + part.maxDelays[outMessageId][causalId];
			
			// Calculate maximum waiting time for each message (saturating
			// addition):
			newMaxTime = newMaxTime > -1 ? newMaxTime : Integer.MAX_VALUE;
			maxTimes[inMessageId] = maxTimes[inMessageId] > newMaxTime ? maxTimes[inMessageId]
					: newMaxTime;
			
			// If causal count is zero, set the bomb to explode:
			if (part.isCausFinal[inMessageId].get(outMessageId)
					&& maxTimes[inMessageId] != Integer.MAX_VALUE) {
				expiryQueue.add(inMessageId);
			}
		}
	}
	
	/**
	 * This method is a receive event that elects all messages pertaining to the
	 * current protocol round. It decides whether the associated message is to
	 * be processed or not and, if yes, it processes it.
	 * 
	 * @param inMessage
	 *            the received message to be tested.
	 * @return true in case the message pertains to an active run.
	 * @throws InterruptedException
	 *             if the running thread is interrupted.
	 * @see CommunicationResource#addReceiveEvent(communications.ReceiveEvent)
	 */
	private boolean ongoingRunEvent(FullMessage<P> inMessage)
			throws InterruptedException {
		// First off, lets filter by protocol name:
		if (!part.protocolName.equals(inMessage.getProtocol())) {
			return false;
		}
		
		// Run id not null: there is an ongoing run.
		if (runId != null) {
			// Check if run ids match:
			if (runId.equals(inMessage.getId())) {
				// If yes, you've go mail! Enqueue message:
				queue.put(inMessage);
				return true;
			} else { // It's the neighbor's mail, perhaps...
				return false;
			}
		} else { // Run id is null: first message from a new run!
			// But first... let's check if it is at all plausible:
			if (part.isInitial(inMessage.getName())) {
				// If yes, you've got mail! Learn new run id and enqueue.
				setRunId(inMessage.getId());
				queue.put(inMessage);
				return true;
			} else { // Something went wrong somewhere...
				return false;
			}
		}
	}
	
	/**
	 * Stops the run by making the actor stop listening to messages.
	 */
	public void exit() {
		communicationResource.removeReceiveEvent(this::ongoingRunEvent);
	}
	
	
	// User API:
	
	/**
	 * Gets the content of a given <em>incoming</em> message. Returns null in
	 * case the message has not arrived.
	 * <p>
	 * Be careful: do not use this method to check if messages have arrived or
	 * not: message contents can be null. Use {@link #hasMessage(String)}
	 * instead.
	 * 
	 * @param messageName
	 *            the name of the message.
	 * @return the content of that message or null if the message was not
	 *         received.
	 * @see #hasMessage(String)
	 */
	public P getMessage(String messageName) {
		short id;
		
		try {
			id = part.inMessageIds.get(messageName);
		} catch (NullPointerException e) {
			throw new NoSuchMessage(String.format(
					"Character %s in protocol %s does not receive %s. %s",
					part.characterName, part.protocolName, messageName,
					part.inMessageIds));
		}
		
		if (accepted.get(id)) {
			return inMessages[id];
		} else {
			return null;
		}
	}
	
	/**
	 * Checks if a given incoming message has arrived.
	 * 
	 * @param messageName
	 *            the name of the message to be checked.
	 * @return true if the message has arrived and is non spurious, false
	 *         otherwise.
	 */
	public boolean hasMessage(String messageName) {
		try {
			return accepted.get(part.inMessageIds.get(messageName));
		} catch (NullPointerException e) {
			throw new NoSuchMessage(String.format(
					"Character %s in protocol %s does not receive %s. %s",
					part.characterName, part.protocolName, messageName,
					part.inMessageIds));
		}
	}
	
	/**
	 * Sets the content of an outgoing message.
	 * 
	 * @param messageName
	 *            the name of the outgoing message.
	 * @param payload
	 *            the payload to be sent with that message.
	 */
	public void setMessage(String messageName, P payload) {
		short id;
		
		try {
			id = part.outMessageIds.get(messageName);
		} catch (NullPointerException e) {
			throw new NoSuchMessage(String.format(
					"Character %s in protocol %s does not send %s. %s",
					part.characterName, part.protocolName, messageName,
					part.outMessageIds));
		}
		
		sent.set(id);
		outMessages[id] = payload;
	}
	
	/**
	 * Gets the <em>current</em> address of a character.
	 * 
	 * @param identifier
	 *            the character's identifier.
	 * @return a String representation of the current address.
	 * @see #getInitialAddress(String)
	 */
	public String getAddress(String identifier) {
		try {
			return addressMap[part.characterIds.get(identifier)];
		} catch (NullPointerException e) {
			throw new NoSuchCharacter(String.format(
					"The protocol %s has no character %s.", part.protocolName,
					identifier));
		}
	}
	
	/**
	 * Sets the address of a character. This method has only effect for this
	 * run; to make the change persistent, use
	 * {@link #setInitialAddress(String, String)}.
	 * 
	 * @param identifier
	 *            the character identifier.
	 * @param address
	 *            the String representation of the address.
	 * @see #setInitialAddress(String, String)
	 */
	public void setAddress(String identifier, String address) {
		try {
			addressMap[part.characterIds.get(identifier)] = address;
		} catch (NullPointerException e) {
			throw new NoSuchCharacter(String.format(
					"The protocol %s has no character %s.", part.protocolName,
					identifier));
			
		}
	}
	
	/**
	 * Gets the <em>initial</em> address of a character.
	 * 
	 * @param identifier
	 *            the character's identifier.
	 * @return the String representation of the character.
	 * @see #getAddress(String)
	 */
	public String getInitialAddress(String identifier) {
		return initialAddressMap.get(identifier);
	}
	
	/**
	 * Sets the address of a character. This method affects the initial state of
	 * all rounds. To make a temporary change to the address map, see
	 * {@link #setAddress(String, String)}.
	 * 
	 * @param identifier
	 *            the character's identifier.
	 * @param address
	 *            the String representation of the address.
	 * @see #setAddress(String, String)
	 */
	public void setInitialAddress(String identifier, String address) {
		try {
			addressMap[part.characterIds.get(identifier)] = address;
			initialAddressMap.put(identifier, address);
		} catch (NullPointerException e) {
			throw new NoSuchCharacter(String.format(
					"The protocol %s has no character %s.", part.protocolName,
					identifier));
		}
	}
	
	/**
	 * Gets the character identifier of the role being enacted by this actor
	 * object.
	 * 
	 * @return the identifier of the character.
	 */
	public String getCharacter() {
		return part.characterName;
	}
	
	/**
	 * Sets the run id. Run ids are attributed automatically, therefore, use
	 * this method only if you really need to. Changing the run id in the middle
	 * of the run will cause unexpected behavior.
	 * 
	 * @param runId
	 *            the new run id.
	 */
	public void setRunId(long runId) {
		this.runId = runId;
	}
	
	/**
	 * Gets the current run id.
	 * 
	 * @return a long representing the id.
	 */
	public long getRunId() {
		return new Long(runId);
	}
}