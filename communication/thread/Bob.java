package thread;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import primitives.Query;
import protocols.ThreadProtocol;
import communications.CommunicationResource;
import communications.FullMessage;
import communications.util.Dump;


public class Bob implements Runnable {
	
	private Map<String, BlockingQueue<FullMessage<String>>> blockingQueues;
	
	public Bob(Map<String, BlockingQueue<FullMessage<String>>> blockingQueues) {
		this.blockingQueues = blockingQueues;
	}
	
	@Override public void run() {
		System.out.println("Bob started.");
		CommunicationResource<String> cp = new ThreadProtocol<String>(
				blockingQueues, "ServerBob");
		
		new Query.Server<String>(new Dump<String>(cp, Dump.Show.SEND), "hello",
				(String str) -> str + ", hohoho!");
	}
}