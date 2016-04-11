package communications;



public interface CommunicationResource<PayloadT> {
	public void addReceiveEvent(ReceiveEvent<PayloadT> receiveEvent);
	public void removeReceiveEvent(ReceiveEvent<PayloadT> receiveEvent);
	public void sendMessage(ShortMessage<PayloadT> msg, String to) throws TxException;
	
	public default String getLocalAddress() {
		return null;
	}
}
