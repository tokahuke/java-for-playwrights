package compose;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.RxException;
import communications.TxException;
import communications.ShortMessage;
import communications.util.QueueResource;
import dsl.Part;


public class Connection<P, M, Q> extends QueueResource<P> {
	
	private static final Random random = new SecureRandom();
	
	private final String protocolName, senderName, receiverName;
	private final Predicate<String> isInitial;
	private final MessageEncodingScheme<P, M> scheme;
	
	private final ExecutorService recevierService;
	private final ExecutorService senderService;
	private final Supplier<SendingActor<M, Q>> senderFactory;
	private final Supplier<ReceivingActor<M, Q>> receiverFactory;
	
	private final Map<String, SendingActor<M, Q>> addressToSenderActor;
	private final HashSet<Long> runningIds = new HashSet<Long>();
	private final BlockingQueue<MessageSenderPair<M>> returnQueue;
	
	/*package-private*/ Connection(TransportPlay<M, Q> play,
			MessageEncodingScheme<P, M> scheme,
			CommunicationResource<Q> resource) {
		this(play, scheme, resource, 10, 10);
	}
	
	/*package-private*/ Connection(TransportPlay<M, Q> play,
			MessageEncodingScheme<P, M> scheme,
			CommunicationResource<Q> resource, int senderMaxNumber,
			int receiverMaxNumber) {
		super();
		
		// Get protocol description for both end points:
		Part senderPart = play.interpretAs(play.getSenderName());
		Part receiverPart = play.interpretAs(play.getReceieverName());
		
		// Get initiality tester:
		this.isInitial = receiverPart::isInitial;
		
		// Get protocol name:
		this.protocolName = play.getProtocolName();
		
		// Internalize character names:
		this.senderName = play.getSenderName();
		this.receiverName = play.getReceieverName();
		
		// Instantiate return queue:
		returnQueue = new ArrayBlockingQueue<MessageSenderPair<M>>(
				receiverMaxNumber + 1);
		
		// Define factories:
		this.senderFactory = () -> {
			SendingActor<M, Q> sender = play.getSender();
			sender.load(senderPart, resource);
			return sender;
		};
		
		this.receiverFactory = () -> {
			ReceivingActor<M, Q> receiver = play.getReceiver();
			receiver.load(receiverPart, resource);
			return receiver;
		};
		
		// Instantiate pools:
		this.recevierService = Executors.newFixedThreadPool(receiverMaxNumber);
		this.senderService = Executors.newFixedThreadPool(senderMaxNumber);
		
		// Instantiate mapping from addresses to id's:
		addressToSenderActor = new ConcurrentHashMap<String, SendingActor<M, Q>>();
		
		// Internalize message encoding scheme:
		this.scheme = scheme;
		
		// Install fresh connection event:
		resource.addReceiveEvent(this::freshRunEvent);
		
		// Activate superclass code:
		super.start();
	}
	
	public boolean freshRunEvent(FullMessage<Q> msg) {
		if (!protocolName.equals(msg.getProtocol())
				|| runningIds.contains(msg.getId())
				|| !isInitial.test(msg.getName()))
			return false;
		
		runningIds.add(msg.getId());
		
		recevierService.execute(() -> {
			ReceivingActor<M, Q> receiver = receiverFactory.get();
			receiver.setQueueAndSenderName(returnQueue, senderName);
			
			try {
				receiver.getMessageQueue().put(msg);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			
			try {
				receiver.perform();
			} catch (Throwable e) {
				// What now!?
			}
		});
		
		return true;
	}
	
	@Override public void sendMessage(ShortMessage<P> msg, String to) {
		SendingActor<M, Q> sender;
		
		synchronized (addressToSenderActor) {
			if (!addressToSenderActor.containsKey(to)) {
				sender = senderFactory.get();
				addressToSenderActor.put(to, sender);

				senderService.execute(() -> {
					Throwable finalOutcome = null;

					sender.setInitialAddress(receiverName, to);
					sender.setRunId(random.nextLong());

					try {
						sender.perform();
					} catch (Throwable e) {
						finalOutcome = e;
					} finally {
						synchronized (addressToSenderActor) {
							sender.setFinalException(finalOutcome);
							addressToSenderActor.remove(to);
						}
					}
				});
			} else {
				sender = addressToSenderActor.get(to);
			}
		}
		// Put the encoded message in the actor:
		try {
			sender.put(scheme.encode(msg));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ParseException e) {
			throw new TxException("Message encoding failed.");
		}
	}
	
	@Override public FullMessage<P> take() throws InterruptedException {
		try {
			MessageSenderPair<M> pair = returnQueue.take();
			return scheme.decode(pair.message).lengthen(pair.sender);
		} catch (ParseException e) {
			throw new RxException("Message decoding failed!");
		}
	}
}