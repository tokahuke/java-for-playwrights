package communications;

@FunctionalInterface public interface ReceiveEvent<PayloadT> {
	public boolean receives(FullMessage<PayloadT> message) throws InterruptedException;
}
