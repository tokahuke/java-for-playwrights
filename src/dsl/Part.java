package dsl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;


/**
 * This class describes what a participant has to do in a protocol. It stores
 * information on other participants's identifiers, message names, message
 * ordering, etc... If the {@link Play} is the centralized description of the
 * global behavior of the network, the Part is the localized description of what
 * each participant has to do. Is is used by the {@link Actor} class to actually
 * run the protocol described in the {@link Play} as one if its characters.
 * 
 * All the fields in this class are package-private and final, thus not
 * accessible to the user.
 * 
 * @author tokahuke
 *
 */
public class Part implements Serializable {
	private static final long serialVersionUID = 1992L;
	
	// Instance members (all package-private):
	
	// Metadata:
	
	/**
	 * The identifier of the character whose part is being modeled.
	 */
	final String characterName;
	
	/**
	 * The name of the protocol being modeled.
	 */
	final String protocolName;
	
	/**
	 * The Actor class for which this part is intended.
	 * 
	 * @see Actor
	 */
	final Class<?> actorClass;
	
	
	// Identities and relations:
	
	/**
	 * The mapping between incoming message names to numeric ids.
	 */
	final Map<String, Short> inMessageIds;
	
	/**
	 * The mapping between outgoing message names to numeric ids.
	 */
	final Map<String, Short> outMessageIds;
	
	/**
	 * The mapping between character identifiers to numeric ids.
	 */
	final Map<String, Short> characterIds;
	
	/**
	 * The mapping between sending character ids to incoming message ids.
	 */
	final short[] characterForMessage;
	
	
	// Distributed flux control:
	
	/**
	 * The next step to be taken when a message is received. It can be an int[],
	 * indicating more messages to be received (this forms the expected messages
	 * graph), or a Node, indicating a final message and the action to be taken
	 * after it is received.
	 */
	final Object[] nextActions;
	
	/**
	 * The reverse of the expected messages graph.
	 */
	final short[][] nextMessagesReverse;
	
	/**
	 * A topological ordering of the nextActions (acyclic) graph.
	 */
	final short[] topSort;
	
	/**
	 * The mapping between incoming message ids and NoRxException handlers.
	 * Null positions indicate no handler was supplied.
	 */
	final Node[] noReceiveHandlers;
	
	/**
	 * The mapping between outgoing messages and the incoming messages that are
	 * caused by each outgoing message.
	 */
	final short[][] causalRelation;
	
	/**
	 * The maximum delay acceptable for each causation pair listed in
	 * {@link #causalRelation}, with the same data structure.
	 */
	final int[][] maxDelays;
	
	/**
	 * Indicates whether a certain out message id in the bit set is a last
	 * message to be sent for the in message id in the array position to become
	 * receivable.
	 */
	final BitSet[] isCausFinal;
	
	/**
	 * 
	 */
	final BitSet isSpontaneous;
	
	// "Constants":
	
	/**
	 * Size of the stack to be used in the search.
	 */
	final int stackSize;
	
	/**
	 * The reference to the first action to be executed (either to do something
	 * or to wait for a message). This makes the execution always begin on an
	 * active Node, even if it is a dummy Node.
	 */
	final RootNode rootNode;

	
	// Constructor:
	
	/* package-private */ Part(String identifier, String protocolName,
			Class<?> actorClass, Map<String, Short> inMessageIds,
			Map<String, Short> outMessageIds, Map<String, Short> characterIds,
			short[] characterForMessage, Object[] nextActions,
			short[][] nextMessagesReverse, short[] topSort, Node[] noReceiveHandlers,
			short[][] causalRelation, int[][] maxDelays, BitSet[] isCausFinal,
			BitSet isSpontaneous, int stackSize, RootNode rootNode) {
		super();
		this.characterName = identifier;
		this.protocolName = protocolName;
		this.actorClass = actorClass;
		this.inMessageIds = inMessageIds;
		this.outMessageIds = outMessageIds;
		this.characterIds = characterIds;
		this.characterForMessage = characterForMessage;
		this.nextActions = nextActions;
		this.nextMessagesReverse = nextMessagesReverse;
		this.topSort = topSort;
		this.noReceiveHandlers = noReceiveHandlers;
		this.causalRelation = causalRelation;
		this.maxDelays = maxDelays;
		this.isCausFinal = isCausFinal;
		this.isSpontaneous = isSpontaneous;
		this.stackSize = stackSize;
		this.rootNode = rootNode;
	}
	
	
	// Methods:
	
	/**
	 * Tests whether a given message name corresponds to an initial message,
	 * i.e., a message to be received before any action is ever taken.
	 * 
	 * @param msgName
	 *            the name of the message.
	 * @return true if the message is initial, false otherwise.
	 */
	 public boolean isInitial(String msgName) {
		Object nextAction = rootNode.getNext().getNextAction();
		
		if(nextAction instanceof BitSet && inMessageIds.containsKey(msgName)) {
			return ((BitSet)nextAction).get(inMessageIds.get(msgName));
		} else {
			return false;
		}
	}
	
	public String toString() {
		return String.format("{\"identifier\": %s,\n"
				+ " \"protocolName\": %s,\n" + " \"inMessageIds\": %s,\n"
				+ " \"outMessageIds\": %s,\n" + " \"characterIds\": %s,\n"
				+ " \"characterForMessage\"  %s,\n" + " \"nextActions\": %s,\n"
				+ " \"nextMessagesReverse\": %s,\n" +" \"causalRelation\": %s,\n" +" \"maxDelays\": %s,\n"
				+ " \"isCausFinal\": %s,\n" + " \"isSpontaneous\": %s,\n"+" \"stackSize\": %d,\n"
				+ " \"rootNode\": %s}", characterName, protocolName,
				inMessageIds.toString(), outMessageIds.toString(),
				characterIds.toString(), Arrays.toString(characterForMessage),
				Arrays.deepToString(nextActions),
				Arrays.deepToString(nextMessagesReverse), Arrays.deepToString(causalRelation),
				Arrays.deepToString(maxDelays), Arrays.deepToString(isCausFinal),isSpontaneous,
				stackSize, rootNode.toString());
	}
}