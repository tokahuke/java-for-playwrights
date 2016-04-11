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
				
				tic = clock.millis();
				CommunicationResource<byte[]> cr = new Dump<byte[]>(
						new WrappedTypeProtocol<String, byte[]>(
								new ThreadProtocol<String>(blockingQueues,
										"221 Baker Street"),
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
				System.out.println(toc-tic);
				
				// Create junk:
				int size = 1492*21;
				byte[] junk = new byte[size];
				new SecureRandom().nextBytes(junk);
				
				// Send junk:
				tic = clock.millis();
				performer.setBulk(junk);
				performer.perform();
				toc = clock.millis();
				
				System.out.println(toc-tic); // delta_t [ms]
				System.out.println(((float)size)/(toc-tic)); // Thr [kB/s]
				System.out.println((toc-tic)/(2.0*size/6.0)); // tp [ms]
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
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		sender.start();
	}
}
