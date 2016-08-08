package segmentation;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;
import communications.CommunicationResource;
import communications.FullMessage;
import communications.util.Dump;
import communications.util.WrappedTypeProtocol;
import dsl.Server;

public class SegmentationShowcase {

	public static void main(String[] args) {
		Map<String, BlockingQueue<FullMessage<String>>> blockingQueues 
			= new HashMap<String, BlockingQueue<FullMessage<String>>>();
		Thread sender, receiver;
		
		// This is Sender:
		sender = new Thread(new Runnable() {
			public void run() {
				Clock clock = Clock.systemUTC();
				long tic, toc;
				ThreadProtocol<String> threadProtocol =
						new ThreadProtocol<String>(blockingQueues,
								"221 Baker Street"); 
				
				tic = clock.millis();
				CommunicationResource<byte[]> cr = new Dump<byte[]>(
						new WrappedTypeProtocol<String, byte[]>(
								threadProtocol,
								str -> "null".equals(str) ? null : Base64
										.getDecoder().decode(str),
								arr -> arr == null ? "null" : Base64
										.getEncoder().encodeToString(arr)),
						arr -> arr == null ? "null" : Base64.getEncoder()
								.encodeToString(arr), Dump.Show.RECEIVE);
						
				Buffered performer = new Buffered();
				performer.load(
						new SimpleTransfer().interpretAs("sender"), cr);
				performer.setInitialAddress("receiver", "103 Addison Road");
				
				toc = clock.millis();
				
				long setupTime = toc - tic;
				
				// Make junk to be sent:
				int size = 1492*21;
				byte[] junk = new byte[size];
				new SecureRandom().nextBytes(junk);
				
				// Send junk:
				tic = clock.millis();
				performer.setBulk(junk);
				performer.perform();
				toc = clock.millis();
				
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					return;
				}

				System.out.println("\nSTATISTICS:\n");
				System.out.printf("System setup time: %dms\n", setupTime);
				System.out.printf("Total transmission time: %dms\n", toc - tic);
				System.out.printf("Throughput: %fMbps\n", ((float) size)
						* 0.001 * 8. / (toc - tic));
				System.out.printf("Latency per message: %fms\n", (float)(toc - tic)
						/ (threadProtocol.getNumberOfTxMessages()));
			}
		});
		
		// This is Receiver:
		receiver = new Thread(new Runnable() {
			public void run() {
				CommunicationResource<byte[]> cr = new Dump<byte[]>(
						new WrappedTypeProtocol<String, byte[]>(
								new ThreadProtocol<String>(blockingQueues,
										"103 Addison Road"),
								str -> "null".equals(str) ? null : Base64
										.getDecoder().decode(str),
								arr -> arr == null ? "null" : Base64
										.getEncoder().encodeToString(arr)),
						arr -> arr == null ? "null" : Base64.getEncoder()
								.encodeToString(arr), Dump.Show.RECEIVE);

				Server<byte[]> continuator = new Server<byte[]>(
						new SimpleTransfer().interpretAs("receiver"), cr,
						Buffered::new, 2);
				continuator.start();
			}
		});
		
		receiver.start();
		
		// Wait a bit:
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		sender.start();
	}
}
