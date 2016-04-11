package compose;

import communications.CommunicationResource;
import dsl.Play;

public abstract class TransportPlay<M, P> extends Play<P> {
	abstract public String getSenderName();
	abstract public String getReceieverName();
	abstract public SendingActor<M, P> getSender();
	abstract public ReceivingActor<M, P> getReceiver();
	
	public <Q> Connection<Q, M, P> compose(MessageEncodingScheme<Q, M> scheme,
			CommunicationResource<P> resource) {
		return new Connection<Q, M, P>(this, scheme, resource);
	}
	
	public <Q> Connection<Q, M, P> compose(MessageEncodingScheme<Q, M> scheme,
			CommunicationResource<P> resource, int senderMaxNumber,
			int receiverMaxNumber) {
		return new Connection<Q, M, P>(this, scheme, resource, senderMaxNumber,
				receiverMaxNumber);
	}
}