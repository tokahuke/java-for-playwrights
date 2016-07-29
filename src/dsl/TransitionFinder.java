package dsl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;

import communications.RxException;

final class Transition {
	final List<Short> messagePath;
	final Node nextNode;
	
	public Transition(List<Short> messagePath, Node nextNode) {
		this.messagePath = messagePath;
		this.nextNode = nextNode;
	}
}

final class TransitionFinder {
	private enum MessageStatus {
		NOT_RECEIVED, RECEIVED, TIMEOUT, TIMEOUT_OPT,
	}
	
	private final Part part;
	private final BitSet initials;
	
	private MessageStatus[] messageStatus;
	
	private boolean foundFinal = false;
	private BitSet explored;
	
	public TransitionFinder(Part part, BitSet initials) {
		this.part = part;
		this.initials = initials;
		
		this.messageStatus = new MessageStatus[part.inMessageIds.size()];
		this.explored = new BitSet();
		
		for (short id = 0; id < messageStatus.length; id++) {
			messageStatus[id] = MessageStatus.NOT_RECEIVED;
		}
	}

	/**
	 * 
	 */
	public Transition markReceived(short messageId) {
		messageStatus[messageId] = MessageStatus.RECEIVED;
		return lookForTransition(messageId);
	}
	
	
	public boolean isReceived(short messageId) {
		return messageStatus[messageId] == MessageStatus.RECEIVED;
	}
	
	/**
	 * 
	 */
	public Transition markTimeout(short messageId) {
		// TRivial case (this will happen many times):
		if (messageStatus[messageId] == MessageStatus.RECEIVED) {
			return null;
		}
		
		if (part.noReceiveHandlers[messageId] == EndNode.NO_RECEIVE) {
			messageStatus[messageId] = MessageStatus.TIMEOUT;
			return null;
		} else {
			messageStatus[messageId] = MessageStatus.TIMEOUT_OPT;
			return lookForTransition(messageId);
		}
	}
	
	public boolean isTimeoutOpt(short messageId) {
		return messageStatus[messageId] == MessageStatus.TIMEOUT_OPT;
	}
	
	/**
	 * Looks for a transition in the message DAG, given that the given message
	 * arrived.
	 * 
	 * @param initials
	 *            the BitSet of initial messages.
	 * @param messageId
	 *            the id of the message.
	 * @return the next active Node to be executed if it exists, else null.
	 */
	private Transition lookForTransition(short messageId) {
		// If a final was not yet found, there is no use finding a transition:
		if (!foundFinal) {
			if (initials.get(messageId)) {
				// Initial are trivially explored:
				foundFinal = searchForFinal(messageId);
			} else {
				// Check if message connects to previously explored:
				for (short previous : part.nextMessagesReverse[messageId]) {
					if (explored.get(previous) && searchForFinal(messageId)) {
						// If connects, do search for a final message:
						if (foundFinal = searchForFinal(messageId)) {
							break;
						}
					}
				}
			}
		}
		
		// If needed, try again:
		if (foundFinal) {
			Transition transition = longestPath();
			
			// Still need to see if a better solution is possible:
			if (isSubPath(transition.messagePath)) {
				return null;
			} else {
				return transition;
			}
		} else { // We will have to wait more...
			return null;
		}
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
	private boolean searchForFinal(short messageId) {
		
		// Search variables initialization:
		Queue<Short> queue =  new ArrayDeque<Short>();
		queue.add(messageId);
		
		// Depth first search:
		while (!queue.isEmpty()) {
			// Dequeue:
			short current = queue.remove();
			explored.set(current);
			
			// Analyze:
			if (part.nextActions[current] instanceof Node) {
				// Found a final message
				return true;
			} else {
				for (short next : (short[]) part.nextActions[current]) {
					// Only put those messages that arrived or timed out in the
					// right conditions:
					if (messageStatus[next] == MessageStatus.RECEIVED
							|| (messageStatus[next] ==
									MessageStatus.TIMEOUT_OPT)) {
						queue.add(next);
					}
				}
			}
		}
		
		// End of the search. Nothing here.
		return false;
	}
	
	private Transition longestPath() {
		List<Short> path = new ArrayList<Short>();
		List<Short> finalsList = new ArrayList<Short>();
		short[] dist = new short[part.inMessageIds.size()];
		short[] parent = new short[part.inMessageIds.size()];
		
		// Longest path in DAG:
		for (short v : part.topSort) {
			if (messageStatus[v] == MessageStatus.RECEIVED
					|| (messageStatus[v] ==
							MessageStatus.TIMEOUT_OPT)) {
				
				for (short u : part.nextMessagesReverse[v]) {
					if (dist[v] < dist[u] + 1) {
						dist[v] = (short) (dist[u] + 1);
						parent[v] = (short) (u + 1);
					}
				}
				
				// If it is final, add to final list:
				if (part.nextActions[v] instanceof Node) {
					finalsList.add(v);
				}
			}
		}
		
		// Find farthest final message:
		Short finalId = null;
		short d = -1;
		
		for (short msgId : finalsList) {
			if (d < dist[msgId]) {
				finalId = msgId;
			}
		}
		
		// Backtrack:
		for (short id = finalId; id != -1; id = (short) (parent[id] - 1)) {
			path.add(id);
		}
		
		return new Transition(path, (Node) part.nextActions[finalId]);
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	private boolean isSubPath(List<Short> path) {
		if (path.size() == 1) {
			// Only message is final.
			return false;
		}
		
		short v = path.get(0);
		
		for(int i = 1; i < path.size(); i++) {
			short u = path.get(i);
			
			// We have got an edge (u, v) in the DAG.
			if (findBackup(u, v)) {
				return true;
			}
			
			v = u;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param u
	 * @param v
	 * @return
	 */
	private boolean findBackup(short u, short v) {
		Queue<Short> queue = new ArrayDeque<Short>();
		BitSet explored = new BitSet(part.inMessageIds.size());
		
		queue.add(u);
		
		while (!queue.isEmpty()) {
			short current = queue.remove();
			explored.set(current);
			
			if (current == v) {
				return true;  
			} else if (part.nextActions[current] instanceof short[]) {
				for (short next : (short[]) part.nextActions[current]) {
					if (messageStatus[next] != MessageStatus.TIMEOUT
							&& (current != u || next != v)) {
						queue.add(next);
					}
				}
			}
		}
		
		// TODO if the only path is made exclusively of TIMEOUT_OPT messages, it
		// does not count as backup path.
		
		return false;
	}
	
	/**
	 * 
	 * @param messageId
	 * @return
	 */
	private boolean notBadTimeout(short messageId) {
		return messageStatus[messageId] != MessageStatus.TIMEOUT;
				// TODO solve this.
				//&& (acceptExpired || messageStatus[messageId] != MessageStatus.TIMEOUT_OPT);
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
	public BitSet getConnectivityProof() {
		
		// Initializing search variables:
		BitSet proof = new BitSet();
		Queue<Short> queue = new ArrayDeque<Short>();
		BitSet explored = new BitSet();
		short[] parent = new short[part.inMessageIds.size()];
		
		// Put initials in stack:
		for (short i = 0; i < initials.length(); i++) {
			if (initials.get(i) && notBadTimeout(i)) {
				queue.add(i);
			}
		}
		
		// Depth first search:
		while (!queue.isEmpty()) {
			// Dequeue:
			short current = queue.remove();
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
					// Only put those messages that may arrive or have timed out
					// correctly:
					if (!explored.get(next) && notBadTimeout(next)) {
						queue.add(next);
						parent[next] = (short) (current + 1);
					}
				}
			}
		}
		
		return proof;
	}
}
