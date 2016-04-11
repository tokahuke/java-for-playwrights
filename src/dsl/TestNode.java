package dsl;

import java.util.function.BiConsumer;

import function.TestableWithActor;

final class TestNode<A extends Actor<?>> extends Node {
	private final TestableWithActor<A> test;
	private final String testName;
	private Node ifTrue = null, ifFalse = null;

	public TestNode(String character, TestableWithActor<A> test, String testName) {
		super();
		this.character = character;
		this.test = test;
		this.testName = testName;
	}

	@Override public Node copy() {
		return new TestNode<A>(character, test, testName);
	}
	
	@Override public void put(Node to, Outcome outcome) {
		switch (outcome) {
			case TRUE:
				ifTrue = to;
				break;
			case FALSE:
				ifFalse = to;
				break;
	
			default:
				throw new IllegalStateException(String.format(
						"Invalid outcome for TestNode: %s", outcome));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override public Node next(Actor<?> actor) {
		return test.test((A)actor) ? ifTrue : ifFalse;
	}

	@Override public void quickCheck() {
		if (ifTrue != EndNode.OK
				&& !ifTrue.character.equals(this.character)) {
			throw new IllegalStateException(String.format(
					"At %s: if-block handled by %s; should be %s.", this,
					ifTrue.character, this.character));
		} else if (ifFalse != EndNode.OK
				&& !ifFalse.character.equals(this.character)) {
			throw new IllegalStateException(String.format(
					"At %s: else-block handled by %s; should be %s.", this,
					ifFalse.character, this.character));
		}
	}
	
	@Override public void forEach(BiConsumer<Node, Outcome> consumer) {
		consumer.accept(ifTrue, Outcome.TRUE);
		consumer.accept(ifFalse, Outcome.FALSE);
	}
	
	@Override public String toString() {
		return "[" + character + " tests "
				+ String.format("%s (%H)", testName, test.getClass().hashCode())
				+ "]";
	}
}