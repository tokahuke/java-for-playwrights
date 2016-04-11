package communications.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.ReceiveEvent;
import communications.TxException;
import communications.ShortMessage;

public class WrappedTypeProtocol<FromT, ToT> implements
		CommunicationResource<ToT> {

	private CommunicationResource<FromT> communicationsResource;
	private Function<FromT, ToT> convert;
	private Function<ToT, FromT> revert;
	
	private Map<ReceiveEvent<ToT>, ReceiveEvent<FromT>> conversionMap =
			new HashMap<ReceiveEvent<ToT>, ReceiveEvent<FromT>>();

	public WrappedTypeProtocol(CommunicationResource<FromT> communicationsResource,
			Function<FromT, ToT> convert, Function<ToT, FromT> revert) {
		this.communicationsResource =  communicationsResource;
		this.convert = convert;
		this.revert = revert;
	}
	

	@Override public void addReceiveEvent(ReceiveEvent<ToT> receiveEvent) {
		ReceiveEvent<FromT> receiveEventConverted = (FullMessage<FromT> msg) -> {
			return receiveEvent.receives(new FullMessage<ToT>(
				msg.getId(),
				msg.getProtocol(),
				msg.getName(),
				msg.getFrom(),
				convert.apply(msg.getPayload()))
			);
		};

		conversionMap.put(receiveEvent, receiveEventConverted);

		communicationsResource.addReceiveEvent(receiveEventConverted);
	}

	@Override public void removeReceiveEvent(ReceiveEvent<ToT> mre) {
		communicationsResource.removeReceiveEvent(conversionMap.get(mre));
		conversionMap.remove(mre);
	}
	
	@Override public void sendMessage(ShortMessage<ToT> msg, String to)
			throws TxException {
		communicationsResource.sendMessage(new ShortMessage<FromT>(
				msg.getId(),
				msg.getProtocol(),
				msg.getName(),
				revert.apply(msg.getPayload())),
				to);
	}
}
