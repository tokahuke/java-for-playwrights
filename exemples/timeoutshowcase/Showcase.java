package timeoutshowcase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;
import theater.Continuator;
import theater.Initiator;
import theater.Instance;
import theater.Play;
import communications.Message;
import exceptions.RxException;

class ExponentialBackoff extends Play<Instance<String>> {

	private StatefulCharacter alice, bob;
	
	public ExponentialBackoff(String mainRole) {
		super(mainRole);
	}
	
	@Override public void dramatisPersonae() {
		alice = newRole("Alice");
		bob = newRole("Bob");
	}

	@Override public void scene() {
		int timeout = 200;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		while(true) {
			alice.run((inst) -> inst.setMessage("MSG", "Hohoho!"));
			alice.send(bob, "MSG");
			
			try {
				bob.run((inst) -> {
					try {
						br.read();
					} catch (Exception e) {}
					
					inst.setMessage("ACK", "Ack!");
				});
				
				bob.send(alice, "ACK", timeout);
				break;
			} catch(RxException e) {
				if (timeout < 5*timeout)
					timeout *= 2;
			}
		}
		 
		System.out.println("Protocol run ended."); 
	}
}

public class Showcase {
	public static void main(String[] args) {
		Map<String, BlockingQueue<Message<String>>> blockingQueues =
				new HashMap<String, BlockingQueue<Message<String>>>();

		// This is BOB:
		new Thread(() -> {
			ThreadProtocol tp = new ThreadProtocol(blockingQueues, "221 Baker Street");
			new Continuator<String, Instance<String>>(new ExponentialBackoff(
					"Bob"), Instance<String>::new, tp).start();
		}, "bob").start();

		// Wait a bit:
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// This is ALICE:
		new Thread(() -> {
			ThreadProtocol tp = new ThreadProtocol(blockingQueues, "103 Addison Road");
			Initiator<String, Instance<String>> initiator = new Initiator<String, Instance<String>>(new ExponentialBackoff("Alice"), tp);

			initiator.setInitialAddress("Bob", "221 Baker Street");

			// Wait a bit:
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			initiator.enact(new Instance<String>());
		}, "alice").start();

		System.out.println("End!");
	}
}
