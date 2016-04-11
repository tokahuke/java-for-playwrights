package threadshowcase;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import primitives.Query;
import protocols.ThreadProtocol;
import communications.CommunicationsResource;
import communications.Message;

public class Bob implements Runnable {
	private Map<String, BlockingQueue<Message<String>>> blockingQueues;
	
	public Bob(Map<String, BlockingQueue<Message<String>>> blockingQueues) {
		this.blockingQueues = blockingQueues;
	}
	@Override public void run() {
		System.out.println("Bob started.");
		CommunicationsResource<String> cp = new ThreadProtocol(blockingQueues, "ServerBob");
		
		new Query.Server<String>(cp, "hello", (String str) -> str + ", hohoho!");
	}
}