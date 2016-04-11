package thread;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import primitives.Query;
import protocols.ThreadProtocol;
import communications.CommunicationResource;
import communications.FullMessage;
import communications.util.Dump;


public class Alice implements Runnable {
	
	private Map<String, BlockingQueue<FullMessage<String>>> blockingQueues;
	
	public Alice(Map<String, BlockingQueue<FullMessage<String>>> blockingQueues) {
		this.blockingQueues = blockingQueues;
	}
	
	@Override public void run() {
		System.out.println("Alice started.");
		CommunicationResource<String> ap = new ThreadProtocol<String>(
				blockingQueues, "ClientAlice");
		
		Query.Client<String> client = new Query.Client<String>(
				new Dump<String>(ap), "hello", "ServerBob");
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		System.out.println(client.query("Hello"));
	}
}