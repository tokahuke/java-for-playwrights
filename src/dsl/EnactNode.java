package dsl;

import java.util.Map;
import java.util.function.BiConsumer;

class EnactNode extends Node {
	public final Flowchart flowchart;
	private final Map<String, String> cast;
	private final String prefix;
	
	private Node next = EndNode.OK,
			noSend = EndNode.NO_SEND,
			noReceive = EndNode.NO_RECEIVE;

	public EnactNode(Flowchart flowchart, Map<String, String> cast,
			String prefix) {
		super();
		this.flowchart = flowchart;
		this.cast = cast;
		this.prefix = prefix;
	}
	
	@Override public Node copy() {
		return new EnactNode(flowchart, cast, prefix);
	}
	
	@Override public void put(Node to, Outcome outcome) {
		switch (outcome) {
			case OK:
				next = to;
				break;
			case NO_SEND:
				noSend = to;
				break;
			case NO_RECEIVE:
				noReceive = to;
				break;
	
			default:
				throw new IllegalStateException(
						"Invalid outcome for SendNode");
		}
	}
	
	@Override public Node next(Actor<?> actor) {
		throw new RuntimeException("Invalid operation for EnactNode");
	}
	
	@Override public void quickCheck() {
		if (noSend != EndNode.NO_SEND
				&& !noSend.character.equals(this.character)) {
			throw new IllegalStateException(String.format(
					"At %s: TxException handled by %s; should be %s.", this,
					noSend.character, this.character));
		}
	}
	
	@Override public void forEach(BiConsumer<Node, Outcome> consumer) {
		consumer.accept(next, Outcome.OK);
		consumer.accept(noSend, Outcome.NO_SEND);
		consumer.accept(noReceive, Outcome.NO_RECEIVE);
	}
	
	@Override public String toString() {
		return "[EnactNode " + cast +  " with prefix " + prefix + "]";
	}

	public void mapEnds() {
		flowchart.mapEnacts(); // recursive step. (no stop condition!!)
		/// Do necessary mappings
		flowchart.getFirst().mapEnds(next, noSend, noReceive);
	}
	
	public Node getFirst() {
		return flowchart.getFirst();
	}
}