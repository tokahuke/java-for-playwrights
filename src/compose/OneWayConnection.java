//package compose;
//
//import java.security.SecureRandom;
//import java.text.ParseException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//
//import theater.Part;
//import communications.CommunicationResource;
//import communications.FullMessage;
//import communications.QueueResource;
//import communications.ShortMessage;
//import exceptions.RxException;
//import exceptions.TxException;
//
//
//public class OneWayConnection<P, M, Q> extends QueueResource<P> {
//	
//	private static Random random = new SecureRandom();
//	
//	private final BlockingQueue<ReceivingActor<M, Q>> returnQueue;
//	private final ActorPool<SendingActor<M, Q>> senderPool;
//	private final ActorPool<ReceivingActor<M, Q>> receiverPool;
//	private final String receiverName, senderName;
//	private final Map<String, Long> addressToId;
//	private final MessageEncodingScheme<P, M> scheme;
//	
//	OneWayConnection(TransportPlay<M, Q> play,
//			MessageEncodingScheme<P, M> scheme,
//			CommunicationResource<Q> resource) {
//		this(play, scheme, resource, 10, Integer.MAX_VALUE);
//	}
//	
//	OneWayConnection(TransportPlay<M, Q> play,
//			MessageEncodingScheme<P, M> scheme,
//			CommunicationResource<Q> resource, int senderMaxNumber,
//			int receiverMaxNumber) {
//		// Get protocol description for both end points:
//		Part senderPart = play.interpretAs(play.getSenderName());
//		Part receiverPart = play.interpretAs(play.getReceieverName());
//		
//		// Instantiate return queue:
//		returnQueue = new ArrayBlockingQueue<ReceivingActor<M, Q>>(
//				receiverMaxNumber + 1);
//		
//		// Instantiate pools:
//		senderPool = new ActorPool<SendingActor<M, Q>>(() -> {
//			SendingActor<M, Q> sender = play.getSender();
//			sender.load(senderPart, resource);
//			return sender;
//		}, senderMaxNumber);
//		
//		receiverPool = new ActorPool<ReceivingActor<M, Q>>(() -> {
//			ReceivingActor<M, Q> receiver = play.getReceiver();
//			receiver.load(receiverPart, resource);
//			return receiver;
//		}, receiverMaxNumber);
//		
//		// Internalize character names:
//		this.senderName = play.getSenderName();
//		this.receiverName = play.getReceieverName();
//		
//		// Instantiate mapping from addresses to id's:
//		addressToId = new HashMap<String, Long>();
//		
//		// Internalize message encoding scheme:
//		this.scheme = scheme;
//		
//		// Install fresh connection event:
//		resource.addReceiveEvent(this::freshRunEvent);
//		
//		// Activate superclass code:
//		super.start();
//	}
//	
//	public boolean freshRunEvent(FullMessage<Q> msg) {
//		synchronized (receiverPool) {
//			if (!receiverPool.isRunningId(msg.getId())) {
//				// Here I only have the ID. The actor is fresh:
//				ReceivingActor<M, Q> receiver = receiverPool.getFreshActor(msg
//						.getId());
//				
//				// Pass the relevant information to the receiving actor:
//				synchronized (receiver) {
//					// In case the actor is brand new:
//					receiver.setReturnQueue(returnQueue);
//					
//					// Put the message for it to receive:
//					try {
//						receiver.getMessageQueue().put(msg);
//					} catch (InterruptedException e) {
//						Thread.currentThread().interrupt();
//						return true;
//					}
//					
//					// Wake the old sleepy head up!
//					receiver.notify();
//				}
//				
//				return true;
//			} else {
//				return false;
//			}
//		}
//	}
//	
//	@Override public void sendMessage(ShortMessage<P> msg, String to) {
//		SendingActor<M, Q> sender;
//		
//		if (!addressToId.containsKey(to)
//				|| (sender = senderPool.getRunningActor(addressToId.get(to))) == null) {
//			// Either there is no id for the address or the Id is invalid. Get
//			// fresh actor to do the job!
//			long id;
//			
//			sender = senderPool.getFreshActor(id = random.nextLong());
//			
//			// Pass the relevant information to the sending actor:
//			synchronized (sender) {
//				sender.setAddress(receiverName, to);
//				sender.setRuntId(id);
//				sender.notify(); // Wake him up!
//			}
//			
//			// Associate that address with this connection id:
//			addressToId.put(to, id);
//		}
//		
//		// Put the encoded message in the actor:
//		try {
//			sender.put(scheme.encode(msg));
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//		} catch (ParseException e) {
//			throw new TxException("Message encoding failed.");
//		}
//	}
//	
//	@Override public FullMessage<P> take() throws InterruptedException {
//		ReceivingActor<M, Q> receiver; 
//		ShortMessage<P> decodedMsg;
//		String senderAddress;
//		
//		// Take a receiver from the queue. He's got a message!
//		receiver = returnQueue.take();
//		
//		// Take and decode the message:
//		try {
//			decodedMsg = scheme.decode(receiver.take());
//		} catch (ParseException e) {
//			throw new RxException("Message decoding failed.");
//		}
//		
//		// Find out who the message is from:
//		senderAddress = receiver.getAddress(senderName);
//		
//		// In case connection is anonymous:
//		if (senderAddress == null) {
//			// Use ID as temporary address:
//			senderAddress = String.format("!%s_%x", senderName, receiver.getRunId());
//		}
//		
//		return decodedMsg.lengthen(senderAddress);
//	}
//}