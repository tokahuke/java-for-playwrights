package dsl;

import java.util.function.BiConsumer;

import function.RunnableWithActor;

final class RunNode<A extends Actor<?>> extends Node {
	private Node next = EndNode.OK;
	private final RunnableWithActor<A> run;
	private final String runName;
	
	RunNode(String actor, RunnableWithActor<A> run, String runName) {
		this.character = actor;
		this.run = run;
		this.runName = runName;
	}
	
	@Override public Node copy() {
		return new RunNode<A>(character, run, runName);
	}
	
	@Override public void put(Node to, Outcome outcome) {
		if (outcome != Outcome.OK) {
			throw new IllegalStateException(String.format(
					"Invalid outcome for RunNode: %s", outcome));
		} else {
			next = to;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override public Node next(Actor<?> actor) {
		run.run((A)actor);
		return next;
	}
	
	@Override public void quickCheck() {

	}
	
	@Override public void forEach(BiConsumer<Node, Outcome> consumer) {
		consumer.accept(next, Outcome.OK);
	}
	
	public String toString() {
		return "[" + character + " runs "
				+ String.format("%s (%H)", runName, run.getClass().hashCode())
				+ "]";
	}
}