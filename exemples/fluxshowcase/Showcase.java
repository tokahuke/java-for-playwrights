package fluxshowcase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;
import theater.Performer;
import theater.Continuator;
import theater.Initiator;
import theater.Play;

import communications.Message;


class Chat extends Play<String> {
	
	Character alice, bob;

	public Chat(String mainCharacter) {
		super(mainCharacter);
	}
	
	@Override public void dramatisPersonae() {
		alice = new Character("Alice");
		bob = new Character("Bob");
	}

	@Override public void scene() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		alice.run(alice -> {
			System.out.printf("Alice> ");
			try {
				alice.setMessage("HI", br.readLine());
			} catch (Exception e) {}
		});

		alice.send(bob, "HI");

		while (bob.test(bob -> !bob.hasMessage("BYE"))) {
			bob.run(bob -> {
				System.out.printf("Bob> ");
				try {
					bob.setMessage("B", br.readLine());
				} catch (Exception e) {}
			});

			bob.send(alice, "B");

			if (alice.test(alice -> alice.getMessage("B").contains("banana"))) {
				alice.run(alice -> {
					System.out.println("Alice> I love bananas!");
					alice.setMessage("A", "I love bananas!");
				});

				alice.send(bob, "A");
			} else if (alice.test((alice) -> alice.getMessage("B").contains(
					"brussel sprout"))) {
				alice.run((alice) -> {
					System.out.println("Alice> Never say that name again!");
					alice.setMessage("A", "Never say that name again!");
				});

				alice.send(bob, "A");
			} else if (alice
					.test(alice -> alice.getMessage("B").equals("exit"))) {
				alice.send(bob, "BYE");
			} else {
				alice.run(alice -> {
					System.out.printf("Alice> ");
					try {
						alice.setMessage("A", br.readLine());
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				alice.send(bob, "A");
			}
		}

		bob.send(alice, "Kk");
		
		bob.run(bob -> System.out.println("She is gone!"));
	}
}

public class Showcase {
	public static void main(String[] args) {
		Map<String, BlockingQueue<Message<String>>> blockingQueues 
				= new HashMap<String, BlockingQueue<Message<String>>>();

		// This is BOB:
		new Thread(
				() -> {
					ThreadProtocol tp = new ThreadProtocol(blockingQueues,
							"221 Baker Street");
					new Continuator<String>(new Chat("Bob"), tp,
							Performer::new, 2).start();
				}, "bob").start();

		// Wait a bit:
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// This is ALICE:
		new Thread(() -> {
			ThreadProtocol tp = new ThreadProtocol(blockingQueues,
					"103 Addison Road");
			Initiator<String> initiator = new Initiator<String>(new Chat(
					"Alice"), tp, new Performer<String>());

			initiator.setInitialAddress("Bob", "221 Baker Street");

				// Wait a bit:
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				initiator.enact();
				System.out.println("Alice ended!");
			}, "alice").start();
	}
}