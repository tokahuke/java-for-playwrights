package dsl;

import java.util.BitSet;
import java.util.function.BiConsumer;

import communications.TxException;

final class SendNode extends Node {
	private final String messageName;
	private final String receiver;
	private final int timeout;
	private Node next = EndNode.OK,
			noSend = EndNode.NO_SEND,
			noReceive = EndNode.NO_RECEIVE;
	private BitSet handlerMessages;

	public SendNode(String sender, String receiver, String name, int timeout) {
		super();
		this.character = sender;
		this.receiver = receiver;
		this.messageName = name;
		this.timeout = timeout;
	}

	public String getMessageName() {
		return messageName;
	}
	
	public String getReceiver() {
		return receiver;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public Node getNoReceveHandler() {
		return noReceive;
	}
	
	public Node getNext() {
		return next;
	}
	
	public Node getNoSendHandler() {
		return noSend;
	}
	
	public BitSet getHandlerMessages() {
		return handlerMessages;
	}

	public void setHandlerMessages(BitSet handlerMessages) {
		this.handlerMessages = handlerMessages;
	}
	
	@Override public Node copy() {
		return new SendNode(character, receiver, messageName, getTimeout());
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
		try {
			actor.sendMessage(receiver, messageName);
			return next;
		} catch (TxException e) {
			if(noSend != EndNode.NO_SEND)
			    return noSend;
			else
				throw e;
		}
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
		return "[" + character + " -> " + receiver + ": " + messageName
				+ "]";
	}
	
	@Override public int hashCode() {
		return messageName.hashCode() + receiver.hashCode();
	}
	
	@Override public boolean equals(Object other) {
		if (other instanceof SendNode) {
			return messageName.equals(((SendNode) other).messageName)
					&& receiver.equals(((SendNode) other).receiver);
		} else {
			return false;
		}
	}
}