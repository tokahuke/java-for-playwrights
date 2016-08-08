package dsl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * 
 * @author tokahuke
 * 
 */
class Flowchart {
	
	// Utilities:
	
	/**
	 * A functional interface to describe the action to be taken for each edge
	 * in a graph search of the flowchart.
	 * 
	 * @author tokahuke
	 * @see Flowchart#doSearch(Predicate, EdgeConsumer)
	 * @see Flowchart#doSearch(EdgeConsumer)
	 * 
	 */
	@FunctionalInterface public static interface EdgeConsumer {
		/**
		 * Performs an operation on an edge of the flowchart.
		 * 
		 * @param node
		 *            the "from" node of the edge. This will be set to null to
		 *            indicate the first node of the search.
		 * @param next
		 *            the "to" node of the edge. This will be the first node of
		 *            the search when <em>node</em> is set to null.
		 * @param outcome
		 *            the value of the edge.
		 */
		public void accept(Node node, Node next, Outcome outcome);
	}
	
	
	// Flowchart structure:
	
	/**
	 * The first node to appear chronologically in the function being analyzed.
	 */
	private Node first;
	
	
	private Map<Node, Set<Node>> reverse;
	
	// Search variables:
	
	/**
	 * Set of all nodes explored that refer to an action of the current
	 * character.
	 */
	private final Set<Node> activeExplored;
	
	
	// Part building variables:
	
	/**
	 * Maps incoming message names to their assigned ids.
	 * 
	 * @see Part#inMessageIds
	 */
	private final Map<String, Short> inMessageIds;

	/**
	 * Maps character identifiers to their assigned ids.
	 * 
	 * @see Part#characterIds
	 */
	private final Map<String, Short> characterIds;

	/**
	 * Maps outgoing message names to their assigned ids.
	 * 
	 * @see Part#outMessageIds
	 */
	private final Map<String, Short> outMessageIds;

	/**
	 * Maps each position to the sending character of the corresponding incoming
	 * message id.
	 * 
	 * @see Part#characterForMessage
	 */
	private final List<Short> characterForMessage;

	/**
	 * Maps each position to the next step to be taken after the corresponding
	 * message was received.
	 * 
	 * @see Part#nextActions
	 */
	private final List<Object> nextActions;
	
	/**
	 * Maps incoming messages to their NoRxException handlers.
	 * 
	 * @see Part#noReceiveHandlers
	 */
	private final List<Node> noReceiveHandlers;
	
	// Options:
	
	/**
	 * Indicates whether debug information should be displayed on screen.
	 */
	private final boolean verbose;
	
	
	// Constructors:
	
	public Flowchart() {
		this(false);
	}
	
	public Flowchart(boolean verbose) {
		this.reverse = new HashMap<Node, Set<Node>>();
		this.activeExplored = new HashSet<Node>();
		this.inMessageIds = new HashMap<String, Short>();
		this.characterIds = new HashMap<String, Short>();
		this.outMessageIds = new HashMap<String, Short>();
		this.characterForMessage = new ArrayList<Short>();
		this.nextActions = new ArrayList<Object>();
		this.noReceiveHandlers = new ArrayList<Node>();
		this.verbose = verbose;
	}
	
	
	// Getters-n'-setters:
	
	/**
	 * Returns the first (top) node in the graph.
	 * 
	 * @return the first node of the flowchart.
	 */
	public Node getFirst() {
		return first;
	}
	
	/**
	 * Puts an edge on the flowchart. If the from node is null, the to node will
	 * be deemed to be the root of the graph.
	 * 
	 * @param from
	 *            the from node of the edge.
	 * @param to
	 *            the to node o the edge.
	 * @param outcome
	 *            the value of the edge.
	 */
	public void putEdge(Node from, Node to, Outcome outcome) {
		// If from is null, to is first node. Period. No disconnected graphs
		// allowed!
		if (from == null) {
			first = to;
			return;
		}

		// Useful debug point:
		if (verbose) {
			System.out.println(from + " >> " + to + " " + outcome);
		}
		
		// Inscribe edge:
		from.put(to, outcome);
	}
	
	
	// Search methods:
	
	/**
	 * Does a BFS search on all nodes in the graph and executes an action with
	 * each edge.
	 * 
	 * @param action
	 *            the action to be performed on each edge.
	 * @see EdgeConsumer
	 * @see #doSearch(Predicate, EdgeConsumer)
	 */
	public void doSearch(EdgeConsumer action) {
		doSearch(node -> true, action);
	}
	
	/**
	 * Does a BFS search on the nodes for which filter yields <em>true</em> and
	 * executes an action with each edge found.
	 * 
	 * @param filter
	 *            a condition under which nodes are included in the search.
	 * @param action
	 *            the action to be performed on each edge.
	 * @see EdgeConsumer
	 * @see #doSearch(EdgeConsumer)
	 */
	public void doSearch(Predicate<Node> filter, EdgeConsumer action) {
		
		// Search variables:
		LinkedList<Node> queue = new LinkedList<Node>();
		Set<Node> explored = new HashSet<Node>();
		Node first = this.first;
		
		// In case EnactNodes are involved:
		if (first instanceof EnactNode) {
			first = ((EnactNode) first).getFirst();
		}
		
		// Beginning of the search:
		queue.add(first);
		action.accept(null, first, Outcome.OK); // Treat first node separately.
		
		// Search loop:
		while(!queue.isEmpty()) {
			Node node = queue.pollFirst();
			
			if (!explored.contains(node) && filter.test(node)) {
				explored.add(node);
				
				node.forEach((next, outcome) -> {
					if (next instanceof EnactNode) {
						next = ((EnactNode) next).getFirst();
					}
					
					queue.add(next);
					action.accept(node, next, outcome);
				});
			}
		}
	}
	
	
	// Copying and mapping methods:
	
	/**
	 * Returns a new copy of the flowchart.
	 * 
	 * @return a new copy of the flowchart.
	 */
	public Flowchart copy() {
		return map(node -> node);
	}
	
	/**
	 * Maps the flowchart into a new flowchart using a mapping function given.
	 * 
	 * @param function
	 *            the mapping function.
	 * @return a new flowchart with the mapped nodes.
	 */
	public Flowchart map(Function<Node, Node> function) {
		Flowchart other = new Flowchart(false);
		Map<Node, Node> map = new HashMap<Node, Node>();
		
		doSearch((node, next, outcome) -> {
			map.putIfAbsent(next, function.apply(next.copy()));
			other.putEdge(map.get(node), map.get(next), outcome);
		});
		
		return other;
	}
	
	/**
	 * Resolves enact nodes into the corresponding flowchart chunks recursively
	 * (deep resolve).
	 */
	public void mapEnacts() {
		// Tie end-points of sub-flowcharts to flowchart:
		doSearch((node, next, outcome) -> {
			if (next instanceof EnactNode && next != null) {
				((EnactNode) next).mapEnds();
			}
		});
		
		// Tie flowchart to sub-flowcharts:
		doSearch((node, next, outcome) -> {
			if (next instanceof EnactNode) {
				putEdge(node, ((EnactNode) next).getFirst(), outcome);
			}
		});
	}

	
	// Part building:
	
	/**
	 * Builds a part object for a given character.
	 * 
	 * @param characterName
	 *            the character identifier.
	 * @param protocolName
	 *            the name of the protocol.
	 * @param actorClass
	 *            the Actor class for which this protocol was intended.
	 * @param causality
	 *            the calculated causality graph for the protocol.
	 * @return a new part object.
	 * @see Part
	 */
	@SuppressWarnings("unchecked") public Part buildPart(String characterName,
			String protocolName, Class<?> actorClass, Causality causality) {
		
		// Create a root node:
		RootNode rootNode = new RootNode(characterName, first);
		
		// Do search:
		searchMyActions(rootNode);
		
		// Now that we are done, discover the number of messages of each kind:
		int inMessageNumber = inMessageIds.size();
		int outMessageNumber = outMessageIds.size();
		
		// Build arrays from lists:
		Object[] nextActionsArray = new Object[inMessageNumber];
		Node[] noReceiveHandlersArray = new Node[inMessageNumber];
		
		for (int i = 0; i < inMessageNumber; i++) {
			if (nextActions.get(i) instanceof List<?>) {
				nextActionsArray[i] = FlowchartUtils
						.listToShortArray((List<Short>) nextActions.get(i));
			} else {
				nextActionsArray[i] = nextActions.get(i);
			}
			
			noReceiveHandlersArray[i] = noReceiveHandlers.get(i);
		}
		
		short[] characterForMessageArray = FlowchartUtils
				.listToShortArray(characterForMessage);
		
		// Build causality relations:
		 Map<SendNode, Map<SendNode, Integer>> maxDelaysMap =
				 causality.findMaxDelays(characterName);
		short[][] causalityRelation = new short[outMessageNumber][];
		int[][] maxDelays = new int[outMessageNumber][];
		
		maxDelaysMap.forEach((outNode, causedMap) -> {
			int causedSize = causedMap.size();
			short[] causedArray = new short[causedSize];
			int[] delayArray = new int[causedSize];
			
			// Damn you Java! I wanted to use forEach here!!!
			int i = 0;
			
			for (Entry<SendNode, Integer> entry : causedMap.entrySet()) {
				causedArray[i] = inMessageIds.get(entry.getKey().getMessageName());
				delayArray[i] = entry.getValue();
				
				i++;
			};
			
			int outMessageId = outMessageIds.get(outNode.getMessageName());
			causalityRelation[outMessageId] = causedArray;
			maxDelays[outMessageId] = delayArray;
		});
		
		// Set final out messages: 
		doSearch((from, to, outcome) -> {
			if (!reverse.containsKey(to)) {
				reverse.put(to, new HashSet<Node>());
			}
			
			if (from != null) {
				reverse.get(to).add(from);
			}
		});
		
		BitSet[] isCausFinal = new BitSet[inMessageNumber];
		
		causality.findFinals(reverse, maxDelaysMap).forEach(
			(inNode, finals) -> {
			BitSet causFinalSet = new BitSet();
			
			finals.forEach((outNode) -> {
				causFinalSet.set(outMessageIds
						.get(((SendNode) outNode).getMessageName()));
			});
						
			isCausFinal[inMessageIds
			            .get(((SendNode) inNode).getMessageName())] = causFinalSet;
		});
		
		for (short i = 0; i < inMessageNumber; i++) {
			if (isCausFinal[i] == null) {
				isCausFinal[i] = new BitSet();
			}
		}
		
		// Find out which messages (if any) arrive without any message being
		// sent:
		BitSet isSpontaneous = new BitSet();
		
		doSearch(
				node -> !( (node instanceof SendNode)
						&& outMessageIds.containsKey( ((SendNode) node)
								.getMessageName()) ),
				(from, to, outcome) -> {
					if (to instanceof SendNode
							&& inMessageIds.containsKey(((SendNode) to)
									.getMessageName())) {
						isSpontaneous.set(inMessageIds.get(((SendNode) to)
								.getMessageName()));
					}
				});

		// Build part!
		return new Part(characterName, protocolName, actorClass, inMessageIds,
				outMessageIds, characterIds, characterForMessageArray,
				nextActionsArray, FlowchartUtils.reverse(nextActionsArray),
				FlowchartUtils.topSort(nextActionsArray,
						FlowchartUtils.reverse(nextActionsArray)),
				noReceiveHandlersArray, causalityRelation, maxDelays,
				isCausFinal, isSpontaneous, inMessageIds.size(), rootNode);
	}
	
	/**
	 * Does the part building search on nodes belonging to the given character.
	 * This method is called recursively from searchOthersActions.
	 * 
	 * @param start
	 *            the place to start the search from.
	 * @see #searchOthersActions(Node, String)
	 * @see #inscribeAndAdd(List, SendNode, String)
	 */
	@SuppressWarnings("unchecked") private void searchMyActions(Node start) {

		LinkedList<Node> queue = new LinkedList<Node>();
		String characterName = start.getCharacter();
		
		// Add start to the queue:
		queue.add(start);
		
		// Do search (BFS):
		while (!queue.isEmpty()) {
			// Take a node:
			Node current = queue.pollFirst();
			
			// If already explored, continue:
			if (current instanceof EndNode || activeExplored.contains(current)) {
				continue;
			}
			
			// Found an active node!
			if(current.getCharacter().equals(characterName)) {
				current.quickCheck();
				activeExplored.add(current);
				current.enqueueAdjs(queue);
				
				// Active send: outgoing message found.
				if (current instanceof SendNode) {
					SendNode sendNode = (SendNode) current;
					
					// Inscribe the message if new:
					if (!outMessageIds.containsKey(sendNode.getMessageName())) {
						outMessageIds.put(sendNode.getMessageName(),
								(short) outMessageIds.size());
					}
					
					// Inscribe character if new:
					if (!characterIds.containsKey(sendNode.getReceiver())) {
						characterIds.put(sendNode.getReceiver(),
								(short) characterIds.size());
					}
					
					// Explore the receive handler:
					/*Node handler = sendNode.getNoReceveHandler();
					if (!(handler instanceof EndNode)) {
						sendNode.setHandlerMessages(FlowchartUtils
								.listToBitSet((List<Short>) searchOthersActions(
										new RootNode(handler.getCharacter(),
												handler), characterName)));
					}*/
				}
			} else { // Passive node!
				Object nextAction;
				
				// In case we just ran into an incoming message:
				if (current instanceof SendNode
						&& ((SendNode) current).getReceiver().equals(characterName)) {
					List<Short> list = new ArrayList<Short>(1);
					inscribeAndAdd(list, (SendNode) current, characterName);
					
					nextAction = list;
				} else { // But normally there are others down the line:
					nextAction = searchOthersActions(current, characterName);
				}
				
				// When we do find messages to be received:
				if (nextAction instanceof List<?>) {
					current.setNextAction(FlowchartUtils
							.listToBitSet((List<Short>) nextAction));
				} else { // When we don't find any messages to be received:
					current.setNextAction(nextAction);
				}
			}
		}
	}
	
	/**
	 * Does the part building search on nodes not belonging to the given actor.
	 * This method is called recursively from searchMyActions and from itself.
	 *
	 * @param start
	 *            the node to start the search from.
	 * @param characterName
	 *            the character identifier.
	 * @return either the next active node to be executed or a collection of
	 *         next messages to be received.
	 * @see #searchMyActions(Node)
	 */
	private Object searchOthersActions(Node start, String characterName) {

		// Search variables:
		LinkedList<Node> queue = new LinkedList<Node>();
		Set<Node> explored = new HashSet<Node>();
		Node nextActiveNode = null;
		
		// Partial search results:
		Set<SendNode> nextMessages = new HashSet<SendNode>();
		List<Short> adjMessageIds = new ArrayList<Short>();
		
		// Put something in the queue:
		if (start instanceof SendNode
				&& ((SendNode) start).getReceiver().equals(characterName)) {
			start.enqueueAdjs(queue);
		} else {
			queue.add(start);
		}
		
		// Do search (BFS):
		while (!queue.isEmpty()) {
			// Dequeue:
			Node current = queue.pollFirst();
			
			// Explore or continue:
			if (current instanceof EndNode || explored.contains(current)) {
				continue;
			}
			
			// Found an active node!
			if (current.getCharacter().equals(characterName)) {
				if (nextActiveNode != null) {
					throw new RuntimeException(
							String.format(
									"Character %s found an ambiguous bifurcation:"
											+ " %s reaches two active nodes: %s and %s.",
									characterName, start, nextActiveNode,
									current));
				} else if (!nextMessages.isEmpty()) {
					throw new RuntimeException(String.format(
							"Character %s found mixed next actions: "
									+ "%s reaches %s", characterName, start,
							current));
				} else {
					nextActiveNode = current;
				}
			} else { // Passive node!
				current.quickCheck();
				explored.add(current);
				
				if (current instanceof SendNode
						&& ((SendNode) current).getReceiver().equals(characterName)) {
					if (nextActiveNode != null) {
						throw new RuntimeException(String.format(
								"Character %s found mixed next actions: "
										+ "%s reaches active %s and passive %s. %s",
								characterName, start, nextActiveNode, current));
					} else {
						// Found next message!
						nextMessages.add((SendNode) current);
					}
				} else {
					current.enqueueAdjs(queue);
				}
			}
		}
		
		// Search done! Let's see what we have found...
		if (nextActiveNode != null) { // Next is an active node!
			searchMyActions(nextActiveNode);
			return nextActiveNode;
		} else if (!nextMessages.isEmpty()){ // There are next messages!
			for (SendNode sendNode : nextMessages) {
				inscribeAndAdd(adjMessageIds, sendNode, characterName);
			}
			
			return adjMessageIds;
		} else { // It's the end.
			return EndNode.OK;
		}
	}
	
	/**
	 * Adds an incoming message to the part description. If the message is new,
	 * gives it a new id and inscribes its sender too, if necessary.
	 * Additionally, it puts it in the next message graph and inscribes its
	 * NoRceiveException handler if any is supplied.
	 * <p>
	 * This function has two recursions: one for the 
	 * 
	 * @param nextMessagesList
	 *            a list of next messages from a preceding message.
	 * @param sendNode
	 *            a node corresponding to an incoming message;
	 * @param characterName
	 *            the name of the current character.
	 * @see #searchOthersActions(Node, String)
	 */
	private void inscribeAndAdd(List<Short> nextMessagesList,
			SendNode sendNode, String characterName) {
		if (!inMessageIds.containsKey(sendNode.getMessageName())) {
			// Inscribe the message if new:
			inMessageIds.put(sendNode.getMessageName(), (short) inMessageIds.size());

			// Inscribe character if new:
			if (!characterIds.containsKey(sendNode.getCharacter())) {
				characterIds.put(sendNode.getCharacter(),
						(short) characterIds.size());
			}

			// Inscribe association:
			characterForMessage.add(inMessageIds.get(sendNode.getMessageName()),
					characterIds.get(sendNode.getCharacter()));
			
			// Put in the list of all next actions (recursive step 1):
			int id = inMessageIds.get(sendNode.getMessageName());
			FlowchartUtils.fillUntil(id, nextActions); // Resize list, if needed
			nextActions.set(id, searchOthersActions(sendNode, characterName));
			
			// Get the NoRxException handler:
			Node noReceiveHandler = sendNode.getNoReceveHandler();
			FlowchartUtils.fillUntil(id, noReceiveHandlers);
			noReceiveHandlers.set(id, noReceiveHandler);

			// In case an exception handler is supplied (recursive step 2):
			/*if (!(noReceiveHandler instanceof EndNode)) {
				if (noReceiveHandler.getCharacter()
						.equals(characterName)) {
					// Continue search from the node found:
					searchMyActions(noReceiveHandler);
				} else {
					throw new RuntimeException(String.format(
							"Character %s found an invalid NoRxException "
									+ "handler: %s is initiating the handler, "
									+ "should be %s. ", characterName,
							noReceiveHandler.getCharacter(), characterName));
				}
			}*/
			
			// Just accept empty exception handlers (by now):
			/*if (noReceiveHandler != EndNode.NO_RECEIVE &&
					!noReceiveHandler.equals(sendNode.getNext())) {
				throw new RuntimeException(String.format(
						"Non-empty RxException catch block are not yet "
								+ "supported. At %s.", sendNode));
			}*/ // We will need to live like this by now.
		}
		
		// Add it to the next message list:
		nextMessagesList
				.add((short) inMessageIds.get(sendNode.getMessageName()));
	}
}

/**
 * Utilities class for Flowchart: array conversion functions.
 * 
 * @author tokahuke
 *
 */
final class FlowchartUtils {
	// Yeah... Java does not have a method for that.
	public static short[] listToShortArray(List<Short> list) {
		if (list == null)
			return new short[0];
		
		short[] array = new short[list.size()];
		
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i).shortValue();
		}
		
		return array;
	}

	public static <T> void fillUntil(int id, List<T> list) {
		for (int i = list.size(); i < id + 1; i++) {
			list.add(i, null);
		}
	}

	public static short[][] reverse(Object[] doubleArray) {
		int length = doubleArray.length;
		List<List<Short>> reverseList = new ArrayList<List<Short>>(length);
		short[][] reverseArray = new short[length][];
		
		for (int i = 0; i < length; i++) {
			reverseList.add(new ArrayList<Short>());
		}
		
		for (int i = 0; i < length; i++) {
			if (doubleArray[i] instanceof short[]) {
				for (short element : (short[]) doubleArray[i]) {
					reverseList.get(element).add((short) i);
				}
			}
		}
		
		for (int i = 0; i < length; i++) {
			reverseArray[i] = listToShortArray(reverseList.get(i)); 
		}
		
		return reverseArray;
	}
	
	public static final BitSet listToBitSet(List<Short> list) {
		BitSet set = new BitSet();
		
		for (short id : list) {
			set.set(id);
		}
		
		return set;
	}
	
	public static short[] topSort(Object[] nextActions, short[][] reverse) {
		short[] topSort = new short[reverse.length];
		short topSortPointer = 0;
		int[] inDegree = new int[reverse.length];
		short[] stack = new short[reverse.length];
		short stackPointer = 0;
		
		// Build in degree:
		for (int i = 0; i < reverse.length; i++) {
			inDegree[i] = reverse[i].length;
			
			if (inDegree[i] == 0) {
				stack[stackPointer++] = (short) i;
			}
		}
		
		// Do top-sort:
		while (stackPointer != 0) {
			short v = stack[--stackPointer];
			
			if (nextActions[v] instanceof short[]) {
				for (short u : (short[]) nextActions[v]) {
					if (inDegree[u] > 1) {
						inDegree[u]--;
					} else {
						stack[stackPointer++] = u;
					}
				}
			}
			
			topSort[topSortPointer++] = v;
		}
		
		return topSort;
	}
	
	private FlowchartUtils() {}
}