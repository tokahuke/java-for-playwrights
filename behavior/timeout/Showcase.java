package timeout;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;

import communications.FullMessage;
import communications.RxException;
import communications.util.Dump;

import dsl.Actor;
import dsl.Play;
import dsl.Server;

class ExponentialBackoff extends Play<String> {
	
	private StatelessCharacter alice, bob;
	
	@SuppressWarnings("serial") private static final class AliceRxException
			extends RxException {}
	
	public ExponentialBackoff() {
		this.protocolName = "backoff";
	}
	
	@Override public void dramatisPersonae() {
		alice = new StatelessCharacter("Alice", new AliceRxException());
		bob = new StatelessCharacter("Bob");
	}
	
	@Override public void scene() {
		while (true) {
			try {
				alice.send(bob, "msg", 500);
				
				bob.run(bob -> {
					try {
						System.in.read();
					} catch (Exception e2) {}
				});
				
				bob.send(alice, "ack", 500);
			} catch (AliceRxException e) {
				continue;
			}
			
			break;
		}
	}
}

public class Showcase {
	public static void main(String[] args) {
		Map<String, BlockingQueue<FullMessage<String>>> blockingQueues = 
				new HashMap<String, BlockingQueue<FullMessage<String>>>();
		
		// This is BOB:
		new Thread(() -> {
			ThreadProtocol<String> tp = new ThreadProtocol<String>(
					blockingQueues, "221 Baker Street");
			Dump<String> dumpTp = new Dump<String>(tp, Dump.Show.RECEIVE);
			
			new Server<String>(new ExponentialBackoff().interpretAs("Bob"),
					dumpTp, Actor::new, 3).start();
		}, "bob").start();
		
		// Wait a bit:
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// This is ALICE:
		new Thread(() -> {
			ThreadProtocol<String> tp = new ThreadProtocol<String>(
					blockingQueues, "103 Addison Road");
			Dump<String> dumpTp = new Dump<String>(tp, Dump.Show.RECEIVE);
			
			Actor<String> actor = new Actor<String>(
					new ExponentialBackoff().interpretAs("Alice"), dumpTp);
			
			actor.setInitialAddress("Bob", "221 Baker Street");
			
			// Wait a bit:
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				actor.perform();
			}, "alice").start();
		
		System.out.println("End!");
	}
}

// Need it for later on:
//int n = 100;
//final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//
//while (true) {
//	alice.send(bob, alice -> "Hohoho!", "MSG" + n, timeout);
//	
//	try {
//		bob.send(alice, bob -> {
//			System.out.println("Here we are!");
//			try {
//				br.read();
//			} catch (Exception e) {}
//			
//			return "ack!";
//		}, "ACK", timeout);
//		
//		break;
//	} catch (RxException e) {
//		alice.run(alice -> System.out.println("Ué...?"));
//		if (n < 501) {
//			n *= 2;
//		} else {
//			alice.send(bob, "I_QUIT");
//			break;
//		}
//	}
//}

// I'm not sure whether this works:
//alice.send(bob, "msg1", 500);
//try {
//	bob.run(bob -> {
//		try {
//			System.in.read();
//		} catch (Exception e) {}
//	});
//	bob.send(alice, "ack", 500);
//} catch (RxException e) {
//	alice.send(bob, "msg2", 500);
//	
//	try {
//		bob.run(bob -> {
//			try {
//				System.in.read();
//			} catch (Exception e2) {}
//		});
//		bob.send(alice, "ack2", 500);
//	} catch (RxException e2) {
//		alice.send(bob, "msg3", 500);
//		bob.run(bob -> {
//			try {
//				System.in.read();
//			} catch (Exception e3) {}
//		});
//	}
//}

// This worked:
//alice.send(bob, "Hi!", 1000);
//
//try {
//	bob.run(bob -> {
//		System.out.println("I am Bob! Press any key to continue...");
//		try {
//			System.in.read();
//		} catch (Exception e) {}
//	});
//	bob.send(alice, "Hi, There!", 1000);
//} catch (AliceRxException e) {
//	//alice.run(alice -> System.out.println("Alice is alone!"));
//	alice.send(bob, "R U there?", 1000);
//	bob.send(alice, "Yes");
//}
//
//alice.send(bob, "Oh! Hi!");
////alice.run(alice -> System.out.println("Alice ended!"));
////bob.run(bob -> System.out.println("Bob ended!"));