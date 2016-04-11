package dsl;

import java.util.function.BiConsumer;


class EndNode extends Node {
	public static final EndNode OK = new EndNode("ok");
	public static final EndNode NO_SEND = new EndNode("noSend");
	public static final EndNode NO_RECEIVE = new EndNode("noReceive");
	
	private String name;
	
	private EndNode(String name) {
		this.name = name;
	}
	
	@Override public Node copy() {
		return this; // Singleton
	}
	
	@Override public void put(Node to, Outcome outcome) {
		throw new IllegalStateException("Putting edge to an EndNode");
	}
	
	@Override public Node next(Actor<?> actor) {
		throw new IllegalStateException("EndNode has no next");
	}
	
	@Override public void quickCheck() {
		
	}
	
	@Override public void forEach(BiConsumer<Node, Outcome> consumer) {
		
	}
	
	@Override public boolean isActiveFor(String characterName) {
		return true;
	}
	
	public String toString() {
		return "EndNode:" + name;
	}
}