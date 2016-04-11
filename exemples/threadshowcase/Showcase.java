package threadshowcase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import communications.Message;

public class Showcase {
	public static void main(String[] args) throws InterruptedException {
		Map<String, BlockingQueue<Message<String>>> blockingQueues = new HashMap<String, BlockingQueue<Message<String>>>();
		
		Alice alice = new Alice(blockingQueues);
		Bob bob = new Bob(blockingQueues);
		
		new Thread(bob).start();
		Thread.sleep(2000);
		new Thread(alice).start();
	}
}
