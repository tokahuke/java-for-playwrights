package dsl;

import java.util.List;
import java.util.function.BiConsumer;


/**
 * This class models one unit of code in the centralized description of the
 * protocol. It is sub-classed for each type of action a character can take in
 * the description (whether a send, a run or a test).
 * 
 * @author rongomai
 *
 */
abstract class Node {
	
	/**
	 * The identifier of the character performing the action described in this
	 * node.
	 */
	protected String character;
	
	/**
	 * The next action for the character to execute after this. If it is null,
	 * action proceeds normally, if it is a Node, execution jumps to this node
	 * and if it is a set of messages, execution halts and waits for one of the
	 * listed messages to arrive.
	 * 
	 * @see Part
	 */
	protected Object nextAction = null;
	
	/**
	 * Returns a fresh copy of the node.
	 * 
	 * @return the reference to the new object.
	 */
	public abstract Node copy();
	
	/**
	 * Creates an edge from this node to the "to" node, with type given by the
	 * outcome provided. The outcome must be valid for the node type.
	 * 
	 * @param to
	 *            the "to" node of the edge created.
	 * @param outcome
	 *            the outcome which takes execution from the current node to the
	 *            next one.
	 */
	public abstract void put(Node to, Outcome outcome);
	
	/**
	 * Executes the action described in this node, using the Actor object
	 * provided and returning the next node in the execution path.
	 * 
	 * @param actor
	 *            the actor on which the action described in this node will be
	 *            performed.
	 * @return the next node to be executed by the actor.
	 */
	public abstract Node next(Actor<?> actor);
	
	/**
	 * Performs a quick (and incomplete) sanity check of the adjacencies. To be
	 * replaced in the future for more efficient code.
	 */
	public abstract void quickCheck();
	
	/**
	 * Checks if the identifier given equals the identifier of this node.
	 * 
	 * @param characterName
	 *            the identifier to be tested.
	 * @return true if both identifiers are equal, false otherwise.
	 */
	public boolean isActiveFor(String characterName) {
		return characterName.equals(characterName);
	}
	
	/**
	 * Executes an action for each outgoing edge from this node.
	 * 
	 * @param consumer
	 *            the action to be executed on each outgoing edge of this node.
	 */
	public abstract void forEach(BiConsumer<Node, Outcome> consumer);
	
	/**
	 * Gets the identifier of the character to which this Node belongs to.
	 * 
	 * @return a String representation of the character identifier.
	 */
	public final String getCharacter() {
		return character;
	}
	
	/**
	 * Gets the next action that the Actor class has to execute after this Node.
	 * It can be a BitSet indicating a set of initial messages to be received or
	 * a next active Node to be executed or null, in case the next node is
	 * indeed the next Node directly under it.
	 * 
	 * @return a reference to the next action. It still needs to be casted.
	 */
	public final Object getNextAction() {
		return nextAction;
	}
	
	/**
	 * Sets the nextAction of this Node.
	 * 
	 * @param nextAction
	 *            either a BitSet of next messages or a Node indicating where to
	 *            go to.
	 * @see #getNextAction() for a fuller explanation.
	 */
	public final void setNextAction(Object nextAction) {
		this.nextAction = nextAction;
	}

	/**
	 * Enqueues all adjacencies from a node, except those reached by
	 * {@link Outcome#NO_RECEIVE}, which are always to be treated as a separate
	 * case in searches.
	 * 
	 * @param queue
	 *            a queue used in a BSF search.
	 */
	public final void enqueueAdjs(List<Node> queue) {
		forEach((node, outcome) -> {
			if (outcome != Outcome.NO_RECEIVE) {
				queue.add(node);
			}
		});	
	}
	
	/**
	 * Substitutes all EndNode for the corresponding given nodes. This is used
	 * to integrate one flowchart into another.
	 * 
	 * @param next
	 *            the node to which to go instead of {@link EndNode#OK}.
	 * @param noSend
	 *            the node to which to go instead of {@link EndNode#NO_SEND}.
	 * @param noReceive
	 *            the node to which to go instead of {@link EndNode#NO_RECEIVE}.
	 */
	public final void mapEnds(Node next, Node noSend, Node noReceive) {
		forEach((node, outcome) -> {
			if (node == EndNode.OK) {
				put(next, Outcome.OK);
			} else if (node == EndNode.NO_SEND) {
				put(noSend, Outcome.NO_SEND);
			} else if (node == EndNode.NO_RECEIVE) {
				put(noReceive, Outcome.NO_RECEIVE);
			}
		});
	}
}