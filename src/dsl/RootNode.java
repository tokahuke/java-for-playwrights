package dsl;

import java.util.function.BiConsumer;

class RootNode extends Node {
	private Node next;
	
	RootNode(String character, Node next) {
		this.character = character;
		this.next = next;
	}
	
	public Node getNext() {
		return next;
	}
	
	@Override public Node copy() {
		return new RootNode(character, next.copy());
	}
	
	@Override public void put(Node to, Outcome outcome) {
		next = to;
	}
	
	@Override public Node next(Actor<?> actor) {
		return next;
	}
	
	@Override public void quickCheck() {
		
	}
	
	@Override public void forEach(BiConsumer<Node, Outcome> consumer) {
		consumer.accept(next, Outcome.OK);
	}
	
	@Override public boolean isActiveFor(String characterName) {
		return true;
	}
	
	public String toString() {
		return "RootNode";
	}
}