package threadshowcase;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import primitives.Query;
import protocols.ThreadProtocol;
import communications.CommunicationsResource;
import communications.Message;

public class Alice implements Runnable {
	private Map<String, BlockingQueue<Message<String>>> blockingQueues;

	public Alice(Map<String, BlockingQueue<Message<String>>> blockingQueues) {
		this.blockingQueues = blockingQueues;
	}
	
	@Override public void run() {
		System.out.println("Alice started.");
		CommunicationsResource<String> ap = new ThreadProtocol(blockingQueues, "ClientAlice");
		
		Query.Client<String> client = new Query.Client<String>(ap, "hello", "ServerBob");
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		System.out.println(client.query("Hello"));
	}	
}