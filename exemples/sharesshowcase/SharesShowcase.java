package sharesshowcase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import protocols.ThreadProtocol;
import stacking.WrappedTypeProtocol;
import theater.Continuator;
import theater.Initiator;
import theater.Instance;
import theater.Play;
import communications.CommunicationsResource;
import communications.Message;
import exceptions.TxException;


class ShareInstance extends Instance<Integer> {
	private int share;
	private int measure;
	private List<Integer> maskedValues;
	
	public ShareInstance(int measure) {
		this.measure = measure;
		this.share = new SecureRandom().nextInt(); 
	}
	
	public int maskedValue() {
		return measure + share;
	}
	
	public void putMasked(int masked) {
		if(maskedValues == null) {
			maskedValues = new LinkedList<Integer>();
		}
		
		maskedValues.add(masked);
	}

	public void addToShare(int accumulatedShare) {
		share += accumulatedShare;
	}
}

/**
 * This class implements the centralized description of the share protocol (or
 * should it be the "The N Smart Meters and the Suspicious Data Concentrator"?).
 * 
 * @author sys
 *
 */
class ShareProtocol extends Play<ShareInstance> {
	public static final String DATA_CONCENTRATOR = "DC";

	private List<StatefulCharacter> smartMeters;
	private StatefulCharacter dataConcentrator;
	private int smartMeterNumber;

	/**
	 * Constructs a ShareProtocol instance for a given number of smart meters.
	 * 
	 * @param smartMeterNumber
	 *            the number of smart meters involved, plus the data
	 *            concentrator. It should be at least 1, since the data
	 *            concentrator is always present.
	 */
	public ShareProtocol(int smartMeterNumber, String mainRole) {
		super(mainRole);
		this.smartMeterNumber = smartMeterNumber;
		this.smartMeters = new LinkedList<StatefulCharacter>();
	}

	/**
	 * Declares the characters involved in the share protocol: one data
	 * concentrator and (smartMeterNumber - 1) smart meters.
	 */
	@Override public void dramatisPersonae() {
		dataConcentrator = newRole("DC");

		for (int i = 0; i < smartMeterNumber; i++) {
			smartMeters.add(newRole(String.valueOf(i)));
		}
	}

	/**
	 * Describes what goes on during a protocol run.
	 */
	@Override public void scene() {

		// [Data concentrator enters the stage, and secretly chooses a random
		// integer, setting it as his "share"]
		dataConcentrator.run((inst) -> inst.setMessage("Share", inst.maskedValue()));

		// Data concentrator [to first smart meter]: take this share I give you.
		// Keep it well. Trudy cannot find it out.
		dataConcentrator.send(smartMeters.get(1), "Share");

		int sender = 1, receiver = 1;
		while (receiver < smartMeterNumber - 1) {
			// [N-th smart meter takes the share and adds it to one of its own
			// making. He also adds his share to the value he measured in secret
			// and calls that his masked value.]
			smartMeters.get(sender).run(
					(inst) -> {
						System.out.println(inst.getRole()
								+ " calculating share....");
						inst.addToShare(inst.getMessage("Share"));
						inst.setMessage("Masked", inst.maskedValue());
					});

			// N-th smart meter [to data concentrator, rather coldly]: Take my
			// masked value, you data concentrator! As your wisdom is great, so
			// is your cunning. The true value, though, you will never know,
			// least you use it for evil end.
			smartMeters.get(sender).send(dataConcentrator, "Masked");
			
			dataConcentrator.run((inst) -> inst.putMasked(inst.getMessage("Masked")));
			
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
					smartMeters.get(sender).send(smartMeters.get(receiver), "Share");
					break;
				} catch (TxException e) {
					// [N-th smart meter tries the next one in the line.]
				}
			}
			
			sender = receiver;
		}

		// Last smart meter [to data concentrator, rather coldly]: Take my
		// masked value, you data concentrator! As your wisdom is great, so
		// is your cunning. The true value, though, you will never know,
		// least you use if for evil end.
		smartMeters.get(sender).run(
				(inst) -> {
					System.out.println(inst.getRole()
							+ " calculating share....");
					inst.addToShare(inst.getMessage("Share"));
					inst.setMessage("Masked", inst.maskedValue());
				});

		// Last smart meter: Take my masked value and the total share, oh data
		// concentrator! Find out the sum of all measures we smart and wise
		// meters have given to you, though each individual value will forever
		// remain concealed from you and your evil plottings.
		smartMeters.get(sender).send(dataConcentrator, "Masked");
		smartMeters.get(sender).send(dataConcentrator,
				"Share");

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
	private List<String> smartMeterAddresses;
	private Initiator<Integer> initiator;

	/**
	 * Constructs the data concentrator.
	 * 
	 * @param smartMeterAddresses
	 *            the list of smart meter addresses.
	 * @param communicationsResource
	 *            the communications protocol to be used to run the protocol.
	 */
	public DataConcentrator(List<String> smartMeterAddresses,
			CommunicationsResource<Integer> communicationsResource) {
		this.smartMeterAddresses = smartMeterAddresses;

		// Initiator object: the object that runs the protocol as an initiator role.
		initiator = new Initiator<Integer>(ShareProtocol.DATA_CONCENTRATOR,
				communicationsResource, new ShareProtocol(smartMeterAddresses.size()));

		// Setting smart meters addresses (they are static and known a priori):
		for (int i = 1; i < smartMeterAddresses.size(); i++) {
			initiator.setInitialAddress(String.valueOf(i), smartMeterAddresses.get(i));
		}
	}

	/**
	 * Runs the share protocol once and returns the aggregated value.
	 * 
	 * @return the aggregated value retrieved from the smart meters.
	 */
	public int aggregate() {
		// Create one run instance:
		ShareProtocol shareProtocol = new ShareProtocol(
				smartMeterAddresses.size());
		
		// Get the initiator object to enact it:
		initiator.enact(shareProtocol);
		
		// The aggregated value is the sum of all masked values less the
		// aggregated share.
		return shareProtocol.getMasked() - shareProtocol.getShare();
	}

	/**
	 * Runs the protocol once in a separate thread and prints the aggregated
	 * data on the screen.
	 */
	public void run() {
		new Thread(
				() -> {
					// Count to 3:
					for (int i = 1; i <= 4; i++) {
						if(i != 4)
							System.out.printf(i+"... ");
						else
							System.out.printf("and... \n");
						
						try {
							Thread.sleep(500);
						} catch (Exception e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
					
					// Run and print the value on the screen:
					System.out.println("\nData concentrator got: " + aggregate());
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
	private Continuator<Integer> continuator;
	private Random random = new SecureRandom();
	private int position;

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
			CommunicationsResource<Integer> communicationsResource) {
		this.position = position;
		
		// Defines the continuator object that runs the protocol as a
		// continuator role. It needs a protocol factory, that provides code on
		// how to set up each run. In our case, we need to create a dummy
		// measurement and set it in the run:
		continuator = new Continuator<Integer>(
				String.valueOf(position),
				() -> {
					// Creates a new run instance:
					ShareProtocol shareProtocol = new ShareProtocol(
							smartMeterAddresses.size());
					
					// Comes up with a dummy measurement:
					int measure = (int) (random.nextGaussian() * 0)+1;
					shareProtocol.setMeasure(measure);
					
					// Return the initialized instance:
					return shareProtocol;
				}, communicationsResource);

		// Setting the address of the data concentrator:
		continuator.setInitialAddress(ShareProtocol.DATA_CONCENTRATOR,
				smartMeterAddresses.get(0));

		// Setting the address of the smart meters:
		for (int i = 1; i < smartMeterAddresses.size(); i++) {
			continuator.setInitialAddress(String.valueOf(i),
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
		// The number of smart meters plus the data concentrator:
		int smartMeterNumber = 19;
		
		// The addresses:
		List<String> addresses = new LinkedList<String>();
		
		// The means of communication: one blocking queue for each thread.
		Map<String, BlockingQueue<Message<String>>> blockingQueues = 
				new HashMap<String, BlockingQueue<Message<String>>>();

		addresses.add("data_concentrator"); // Yep, this is its address.
		
		for (int n = 1; n < smartMeterNumber; n++)
			addresses.add("smart_meter_" + n); // The smart meters' address.
		
		// This is the DATA CONCENTRATOR:
		new DataConcentrator(addresses, new WrappedTypeProtocol<String, Integer>(
				new ThreadProtocol(blockingQueues, "data_concentrator"),
				(String str) -> Integer.parseInt(str),
				(Integer i) -> String.valueOf(i))).run();

		// These are the SMART METERS, some of which were ensnared by Trudy in a
		// magical sleep:
		for (int n = 1; n < smartMeterNumber; n++) {
			if(n != 7 && n != 8 && n != 11)
				new SmartMeter(addresses, n, new WrappedTypeProtocol<String, Integer>(
					new ThreadProtocol(blockingQueues, "smart_meter_" + n),
					(String str) -> Integer.parseInt(str),
					(Integer i) -> String.valueOf(i))).run();
		
		// Everything should start working and shooting messages around now.
		}
	}
}
