package shares;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;

import communications.CommunicationResource;
import communications.FullMessage;
import communications.RxException;
import communications.ShortMessage;
import communications.TxException;
import communications.util.Dump;
import compose.MessageEncodingScheme;
import composition.AcknowledgeChannel;

import dsl.Actor;
import dsl.Play;
import dsl.Server;


class SmartMeterActor extends Actor<Integer> {
	private int share;
	private int totalShare = 0;
	private int measure;
	
	public SmartMeterActor(int measure) {
		this.measure = measure;
		this.share = new SecureRandom().nextInt();
	}
	
	public int maskedValue() {
		return measure + share;
	}

	public void addToShare(int accumulatedShare) {
		totalShare = accumulatedShare + share;
	}

	public void setMeasure(int measure) {
		this.measure = measure;
	}
	
	public int getTotalShare() {
		return totalShare;
	}
}

class ConcentratorActor extends Actor<Integer> {
	private int share;
	private final int smartMeterNumber;
	
	public ConcentratorActor(int smartMeterNumber) {
		this.share = new SecureRandom().nextInt();
		this.smartMeterNumber = smartMeterNumber;
	}
	
	public int maskedValue() {
		return share;
	}


	public int getAggregate() {
		int totalMasked = 0;
		
		for (int i = 1; i < smartMeterNumber - 1; i++) {
			if (hasMessage("masked-from-" + i)) {
				totalMasked += getMessage("masked-from-" + i);
			}
		}
		
		totalMasked += getMessage("masked-final");
		
		return totalMasked - getMessage("share-final") + share;
	}
}

/**
 * This class implements the centralized description of the share protocol (or
 * should it be the "The N Smart Meters and the Suspicious Data Concentrator"?).
 * 
 * @author sys
 *
 */
class ShareProtocol extends Play<Integer> {
	public static final String DATA_CONCENTRATOR = "DC";
	
	private List<Character<SmartMeterActor>> smartMeters;
	private Character<ConcentratorActor> dataConcentrator;
	private int smartMeterNumber;

	/**
	 * Constructs a ShareProtocol instance for a given number of smart meters.
	 * 
	 * @param smartMeterNumber
	 *            the number of smart meters involved, plus the data
	 *            concentrator. It should be at least 1, since the data
	 *            concentrator is always present.
	 */
	public ShareProtocol(int smartMeterNumber) {
		super.protocolName = "smap";
		this.smartMeterNumber = smartMeterNumber;
		this.smartMeters = new LinkedList<Character<SmartMeterActor>>();
	}

	/**
	 * Declares the characters involved in the share protocol: one data
	 * concentrator and (smartMeterNumber - 1) smart meters.
	 */
	@Override public void dramatisPersonae() {
		dataConcentrator = new Character<ConcentratorActor>(
				ConcentratorActor.class, "DC");

		for (int i = 0; i < smartMeterNumber; i++) {
			smartMeters.add(new Character<SmartMeterActor>(
					SmartMeterActor.class, "sm" + String.valueOf(i)));
		}
	}

	/**
	 * Describes what goes on during a protocol run.
	 */
	@Override public void scene() {
		// [Data concentrator enters the stage, and secretly chooses a random
		// integer, setting it as his "share"]
		//
		// Data concentrator [to first smart meter]: take this share I give you.
		// Keep it well. Trudy cannot find it out.
		dataConcentrator.send(smartMeters.get(1),
				ConcentratorActor::maskedValue, SmartMeterActor::addToShare,
				"share", 10);

		int sender = 1, receiver = 1;
		while (sender < smartMeterNumber - 1) {
			// [N-th smart meter takes the share and adds it to one of its own
			// making. He also adds his share to the value he measured in secret
			// and calls that his masked value.]
			//
			// N-th smart meter [to data concentrator, rather coldly]: Take my
			// masked value, you data concentrator! As your wisdom is great, so
			// is your cunning. The true value, though, you will never know,
			// least you use it for evil end.
			//try {
				smartMeters.get(sender).send(
						dataConcentrator,
						sm -> {
							return sm.maskedValue();
						}, "masked-from-" + sender, 10);
			//} catch (RxException e) {}
			
			//dataConcentrator.run(dc -> dc.putMasked(dc.getMessage("Masked_")));
			
			// [N-th smart meter tries to send the secret share to the next
			// smart meter, whom he trust. If he finds out that Trudy gave him a
			// magic potion and is now in a magical sleep, he tries the next
			// one, and so forth until the end.]
			while (receiver < smartMeterNumber - 1) {
				try {
					receiver++;
					// N-th smart meter: Take this accumulated share to which I
					// and others before me have added to. Add to it one of your
					// own, friend, and be at peace: your secrete is safe with
					// you.
					smartMeters.get(sender).send(smartMeters.get(receiver),
							SmartMeterActor::getTotalShare,
							SmartMeterActor::addToShare, "share-from-" + sender, 10);
					break;
				} catch (TxException e) {
					// [N-th smart meter tries the next one in the line.]
					smartMeters.get(sender).send(dataConcentrator,
							"comm-failed-" + sender + "-" + receiver);
					
					// And if we get to the end, we are screwed!
					if (receiver == smartMeterNumber - 1) {
						return;
					}
				}
			}
			
			sender = receiver;
		}

		// Last smart meter [to data concentrator, rather coldly]: Take my
		// masked value, you data concentrator! As your wisdom is great, so
		// is your cunning. The true value, though, you will never know,
		// least you use if for evil end.
		smartMeters.get(sender).send(dataConcentrator,
				SmartMeterActor::maskedValue, "masked-final", 10);

		// Last smart meter: Take my masked value and the total share, oh data
		// concentrator! Find out the sum of all measures we smart and wise
		// meters have given to you, though each individual value will forever
		// remain concealed from you and your evil plottings.
		smartMeters.get(sender).send(dataConcentrator, (sm) -> sm.getTotalShare(),
				"share-final", 10);
		
		// [The curtains fall.]
	}
}

/**
 * This class implements the code to be run in the data concentrator side. It
 * sets up the protocol and offers a method to trigger the run of the protocol.
 * It also offers a run method that creates a thread to run the protocol once
 * and print the aggregated value on the screen.
 * 
 * @author sys
 *
 */
class DataConcentrator {
	private ConcentratorActor concentratorActor;
	
	/**
	 * Constructs the data concentrator.
	 * 
	 * @param smartMeterAddresses
	 *            the list of smart meter addresses.
	 * @param communicationsResource
	 *            the communications protocol to be used to run the protocol.
	 */
	public DataConcentrator(List<String> smartMeterAddresses,
			CommunicationResource<Integer> communicationsResource) {
		Clock clock = Clock.systemUTC();
		long tic, toc;
		
		tic = clock.millis();
		// Initiator object: the object that runs the protocol as an initiator
		// role.
		concentratorActor = new ConcentratorActor(smartMeterAddresses.size());
		concentratorActor.load(new ShareProtocol(smartMeterAddresses.size())
				.interpretAs(ShareProtocol.DATA_CONCENTRATOR),
				communicationsResource);
		
		// Setting smart meters addresses (they are static and known a priori):
		for (int i = 1; i < smartMeterAddresses.size(); i++) {
			concentratorActor.setInitialAddress("sm" + String.valueOf(i),
					smartMeterAddresses.get(i));
		}
		toc = clock.millis();
		
		System.out.println(String.format("DC setup time: %dms\n", toc-tic));
	}
	
	/**
	 * Runs the share protocol once and returns the aggregated value.
	 * 
	 * @return the aggregated value retrieved from the smart meters.
	 */
	public int aggregate() {
		Clock clock = Clock.systemUTC();
		long tic, toc;
		
		tic = clock.millis();
		// Get the initiator object to enact it:
		concentratorActor.perform();
		toc = clock.millis();
		
		System.out.println();
		System.out.println(String.format("Run time from DC: %dms", toc-tic));
		
		// The aggregated value is the sum of all masked values less the
		// aggregated share.
		return concentratorActor.getAggregate();
	}
	
	/**
	 * Runs the protocol once in a separate thread and prints the aggregated
	 * data on the screen.
	 */
	public void run() {
		new Thread(() -> {
			// Count to 3:
				for (int i = 1; i <= 4; i++) {
					/*if (i != 4)
						System.out.printf(i + "... ");
					else
						System.out.printf("and... \n");
					*/
					try {
						Thread.sleep(500);
					} catch (Exception e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				
				// Run and print the value on the screen:
				System.out.println("Data concentrator got: " + aggregate());
			}, "data_concentrator").start();
	}
}

/**
 * This class implements the code to be run at the smart meters side. It sets up
 * the protocol and offers a start method to make the smart meter start
 * listening to the protocol. It also offers a run method that does this in a
 * new thread.
 * 
 * @author sys
 *
 */
class SmartMeter {
	private Server<Integer> continuator;
//	private Random random = new SecureRandom();

	/**
	 * Constructs the smart meter.
	 * 
	 * @param smartMeterAddresses
	 *            the list of smart meters addresses.
	 * @param position
	 *            the position of the smart meter in the list.
	 * @param communicationsResource
	 *            the communications protocol to be used to run the protocol.
	 */
	public SmartMeter(List<String> smartMeterAddresses, int position,
			CommunicationResource<Integer> communicationsResource) {
		int smartMeterNumber = smartMeterAddresses.size(); 
		// Defines the server object that runs the protocol as a
		// server role. It needs a protocol factory, that provides code on
		// how to set up each run. In our case, we need to create a dummy
		// measurement and set it in the run:
		continuator = new Server<Integer>(
				new ShareProtocol(smartMeterNumber).interpretAs("sm" + String
						.valueOf(position)), communicationsResource, () -> {
				// Creates a new run instance:
				SmartMeterActor smartMeter = new SmartMeterActor(
						smartMeterAddresses.size());
				
				// Comes up with a dummy measurement:
				int measure = 1000;//(int) (random.nextGaussian() * 0) + 1;
				smartMeter.setMeasure(measure);
				
				// Return the initialized instance:
				return smartMeter;
			}, 4);

		// Setting the address of the data concentrator:
		continuator.setInitialAddress(ShareProtocol.DATA_CONCENTRATOR,
				smartMeterAddresses.get(0));

		// Setting the address of the smart meters:
		for (int i = 1; i < smartMeterAddresses.size(); i++) {
			continuator.setInitialAddress("sm" + String.valueOf(i),
					smartMeterAddresses.get(i));
		}
	}

	/**
	 * This function makes the smart meter start listening to the share
	 * protocol.
	 */
	public void start() {
		continuator.start();
	}
	
	/**
	 * Runs the smart meter.
	 */
	public void run() {
		start();
	}
}

/**
 * This class sets up the whole thing: it initializes the data concentrator, the
 * smart meters and the thread protocol used for the simulation. It then puts
 * everything to run once.
 * 
 * @author sys
 *
 */
public class SharesShowcase {
	
	public static void main(String[] args) {
		/*// The number of smart meters plus the data concentrator:
		int smartMeterNumber = 11;
		
		// The addresses:
		List<String> addresses = new LinkedList<String>();
		
		// The means of communication: one blocking queue for each thread.
		Map<String, BlockingQueue<FullMessage<ShortMessage<Integer>>>> blockingQueues =
				new HashMap<String, BlockingQueue<FullMessage<ShortMessage<Integer>>>>();
		
		addresses.add("dc");
		
		for (int n = 1; n < smartMeterNumber; n++)
			addresses.add("meter-" + n); // The smart meters' address.
		
		// This is the DATA CONCENTRATOR:
		CommunicationResource<Integer> concentratorResource =
				new AcknowledgeChannel<ShortMessage<Integer>>().compose(
						MessageEncodingScheme.getTrivialScheme(),
						new Dump<ShortMessage<Integer>>(
								new ThreadProtocol<ShortMessage<Integer>>(
										blockingQueues, "dc"),
								Dump.Show.SEND));
		
		new DataConcentrator(addresses, concentratorResource).run();
		
		// These are the SMART METERS, some of which were ensnared by Trudy in a
		// magical sleep:
		for (int n = 1; n < smartMeterNumber; n++) {
			if (n != 4 && n != 5 && n != 8) {
				CommunicationResource<Integer> resource =
						new AcknowledgeChannel<ShortMessage<Integer>>().compose(
								MessageEncodingScheme.getTrivialScheme(),
								new Dump<ShortMessage<Integer>>(
										new ThreadProtocol<ShortMessage<Integer>>(
												blockingQueues, "meter-" + n),
										Dump.Show.SEND));
				
				new SmartMeter(addresses, n, resource).run();
			}
			
			// Everything should start working and shooting messages around now.
		}*/
		
		// The number of smart meters plus the data concentrator:
		int smartMeterNumber = 11;

		// The addresses:
		List<String> addresses = new LinkedList<String>();

		// The means of communication: one blocking queue for each thread.
		Map<String, BlockingQueue<FullMessage<Integer>>> blockingQueues =
				new HashMap<String, BlockingQueue<FullMessage<Integer>>>();

		addresses.add("dc");

		for (int n = 1; n < smartMeterNumber; n++)
			addresses.add("meter-" + n); // The smart meters' address.

		// This is the DATA CONCENTRATOR:
		CommunicationResource<Integer> concentratorResource = new Dump<Integer>(
				new ThreadProtocol<Integer>(blockingQueues, "dc"),
				Dump.Show.RECEIVE);

		new DataConcentrator(addresses, concentratorResource).run();

		// These are the SMART METERS, some of which were ensnared by Trudy in a
		// magical sleep:
		for (int n = 1; n < smartMeterNumber; n++) {
			if (n != 4 && n != 5 && n != 8) {
				CommunicationResource<Integer> resource = new Dump<Integer>(
						new ThreadProtocol<Integer>(blockingQueues,
								"meter-" + n), Dump.Show.RECEIVE);

				new SmartMeter(addresses, n, resource).run();
			}
		}
		
		// Everything should start working and shooting messages around now.
	}
}
