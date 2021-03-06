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
import communications.ShortMessage;
import communications.util.WrappedTypeProtocol;
import compose.MessageEncodingScheme;
import composition.AcknowledgeChannel;

import dsl.Server;

public class SegmentationShowcase {

	public static void main(String[] args) {
		Map<String, BlockingQueue<FullMessage<ShortMessage<String>>>> blockingQueues 
			= new HashMap<String, BlockingQueue<FullMessage<ShortMessage<String>>>>();
		Thread sender, receiver;
		
		// This is Sender:
		sender = new Thread(new Runnable() {
			public void run() {
				Clock clock = Clock.systemUTC();
				long tic, toc;
				
				tic = clock.millis();
				ThreadProtocol<ShortMessage<String>> threadProtocol =
						new ThreadProtocol<ShortMessage<String>>(blockingQueues,
								"221 Baker Street"); 
				
				CommunicationResource<String> ackThreadProtocol = 
						new AcknowledgeChannel<ShortMessage<String>>().compose(
								MessageEncodingScheme.getTrivialScheme(),
								threadProtocol);
				
				CommunicationResource<byte[]> cr = //new Dump<byte[]>(
						new WrappedTypeProtocol<String, byte[]>(
								ackThreadProtocol,
								str -> "null".equals(str) ? null : Base64
										.getDecoder().decode(str),
								arr -> arr == null ? "null" : Base64
										.getEncoder().encodeToString(arr));//,
					//	arr -> arr == null ? "null" : Base64.getEncoder()
					//			.encodeToString(arr), Dump.Show.RECEIVE);
						
				Buffered performer = new Buffered();
				performer.load(
						new SimpleTransfer().interpretAs("sender"), cr);
				performer.setInitialAddress("receiver", "103 Addison Road");
				
				toc = clock.millis();
				
				long setupTime = toc - tic;
				
				// Make junk to be sent:
				int size = 200_000_00;
				byte[] junk = new byte[size];
				new SecureRandom().nextBytes(junk);
				System.out.println("Begin!");
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
				System.out.printf("Latency per message: %fms\n", 2.0*(float)(toc - tic)
						/ (threadProtocol.getNumberOfTxMessages()));
			}
		});

		// This is Receiver:
		receiver = new Thread(new Runnable() {
			public void run() {
				
				ThreadProtocol<ShortMessage<String>> threadProtocol =
						new ThreadProtocol<ShortMessage<String>>(blockingQueues,
								"103 Addison Road"); 
				
				CommunicationResource<String> ackThreadProtocol = 
						new AcknowledgeChannel<ShortMessage<String>>().compose(
								MessageEncodingScheme.getTrivialScheme(),
								threadProtocol);
				
				
				CommunicationResource<byte[]> cr =// new Dump<byte[]>(
						new WrappedTypeProtocol<String, byte[]>(
								ackThreadProtocol,
								str -> "null".equals(str) ? null : Base64
										.getDecoder().decode(str),
								arr -> arr == null ? "null" : Base64
										.getEncoder().encodeToString(arr));//,
					//	arr -> arr == null ? "null" : Base64.getEncoder()
					//			.encodeToString(arr), Dump.Show.RECEIVE);

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
