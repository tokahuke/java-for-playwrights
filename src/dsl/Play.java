package dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;

import communications.NoSuchCharacter;
import communications.RxException;
import communications.TxException;

import function.ConsumerWithActor;
import function.RunnableWithActor;
import function.SupplierWithActor;
import function.TestableWithActor;
import function.UnaryWithActor;

/**
 * 
 * @author rongomai
 *
 * @param P
 *            the type of payload the play uses to send messages.
 */
public abstract class Play<P> {
	/**
	 * A private exception for catching loops in the running graph.
	 * 
	 * @author rongomai
	 *
	 */
	private static class LoopFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * This method declares all characters that act in the play.
	 */
	public abstract void dramatisPersonae();

	/**
	 * This method describes how characters interact with each other. The
	 * execution of the actions listed might be asynchronous, but must always be
	 * causal. The actual execution might happen in a different order, but this
	 * order will belong to the same equivalence class that the description
	 * given in this function.
	 */
	public abstract void scene();

	/**
	 * The name of the protocol implemented by the class. Should preferably be
	 * overridden.
	 */
	protected String protocolName = this.getClass().getCanonicalName();
	
	/**
	 * The maximum amount time each participant will stay processing a run.
	 */
	protected final int timeout = Integer.MAX_VALUE;
	
	/**
	 * The mapping from character to the Actor class it uses.
	 */
	private final Map<String, Class<?>> actorClasses = 
			new HashMap<String, Class<?>>();
	
	/**
	 * The automaton describing all the actions in the network. Be careful:
	 * since some actions may take place asynchronously, this automaton is only
	 * a representative of its equivalence class.
	 */
	private Flowchart flowchart = null;
	
	/**
	 * The object describing the causality and bifurcations relations in the
	 * protocol. It is used to determine the maximum timeouts for messages.
	 */
	private Causality causality = new Causality();

	
	// Variables for the search:
	
	/**
	 * A modified stack that records not the nodes to be explored, but the path
	 * that leads to them. This is necessary, since the code needs to know how
	 * to drive the function to get there when the search backtracks.
	 */
	private LinkedList<ArrayList<Outcome>> searchStack;
	
	/**
	 * The standard, normal search stack.
	 */
	private LinkedList<Node> lastNodeStack;

	/**
	 * The stack used to track tips configurations at every step.
	 */
	private LinkedList<Map<String, Tip>> tipsStack;
	
	/*
	 * This is the map of explored nodes.
	 */
	private Map<Object, Node> exploredNodes;

	/**
	 * Current path to the node.
	 */
	private ArrayList<Outcome> decisionList;
	
	/*
	 * This index in the list of nodes that leads to the last node.
	 */
	private int listIndex;
	
	/**
	 * The last node explored.
	 */
	private Node lastNode;
	
	/**
	 * Protected empty constructor.
	 */
	
	
	// Constructors:
	
	protected Play() {
	
	}
	
	
	// Other methods:
	
	/**
	 * Get the name of the protocol implemented by this class.
	 * 
	 * @return a string uniquely identifying the protocol.
	 */
	public final String getProtocolName() {
		return protocolName;
	}
	
	/**
	 * Explores the method scene and builds a graph of the calls to send, run
	 * and test. The problem with this search is that, since we are searching
	 * running code, every time we want to backtrack, we need to throw an
	 * exception and start all over again until we get to the point we should
	 * get to. It seems that DFS is the best search to handle this inconvenient,
	 * but that still remains to be proved.
	 * 
	 * TODO bad description.
	 * 
	 * @param mainCharacter
	 *            the character the program has to enact.
	 * @return the part object containing all information to relative to the
	 *         role of the given character.
	 */
	public final Part interpretAs(String mainCharacter) {
		Flowchart flowchart = getFlowchart();
		
		/*Tip.explored = new HashSet<Tip>();
		
		for (Tip tip: causality.copyInitialTips().values()) {
			tip.dbgPrint();
			System.out.println("--");
		}*/
		
		// See if our star is there after all:
		if (!actorClasses.containsKey(mainCharacter)) {
			throw new NoSuchCharacter(String.format(
					"Character \"%s\" not in %s.", mainCharacter,
					actorClasses.keySet()));
		}
		
		return flowchart.copy().buildPart(mainCharacter, protocolName,
				actorClasses.get(mainCharacter), causality);
	}
	
	/**
	 * Builds the flowchart. This method is the one that actually does all the
	 * fun stuff.
	 * 
	 * @return the flowchart defined by the class.
	 */
	private Flowchart getFlowchart() {
		if (flowchart != null) {
			return flowchart;
		}

		// Populate the stage:
		dramatisPersonae();
		
		// Initialize variables:
		searchStack = new LinkedList<ArrayList<Outcome>>();
		searchStack.add(new ArrayList<Outcome>());
		searchStack.peek().add(Outcome.TRUE); // Dummy...

		lastNodeStack = new LinkedList<Node>();
		lastNodeStack.add(null);

		tipsStack = new LinkedList<Map<String, Tip>>();
		tipsStack.add(causality.copyInitialTips());
		
		exploredNodes = new HashMap<Object, Node>();

		flowchart = new Flowchart();
		
		// Do the search (a modified DFS):
		while(!searchStack.isEmpty()) {
			// Setting (reseting) search:
			decisionList = searchStack.pop();
			lastNode = lastNodeStack.pop();
			causality.setTips(tipsStack.pop());
			
			listIndex = 1;
			
			// Running it: (implementation of the search continues in the send,
			// run and test methods):
			try {
				scene();
			} catch (LoopFoundException | TxException | RxException e) {}
		}
	
		return flowchart;
	}
	
	/**
	 * A method to be called during the search to manage all the search
	 * overhead. What it basically does is to avoid the action from being
	 * executed in case the search is in backtracking mode (remember: this is
	 * not a normal graph and this is not a normal search.).
	 * 
	 * @param fullName
	 *            an unique identifier to the function call. This is the
	 *            parameter used to discover in which state of execution we are.
	 * @param action
	 *            the action to be executed during the search (pretending it is
	 *            a normal search).
	 * @return the <em>effect</em> of the function call (it might also be
	 *         throwing an exception), but never mind what that means...
	 * @see #leaveForLater(Node, Outcome, Map)
	 */
	private Boolean onSearch(Object fullName, Supplier<Outcome> action,
			TxException noSend, RxException noReceive) {
		// Here is the idea: if we have reached the end of the decisionList,
		// we "do stuff" with the node, else, we pass and increment until
		// we get somewhere.
		if (listIndex == decisionList.size()) {

			// If the node is fresh, we put it to the automaton graph, mark
			// it as visited and push it to the list (note that here we need
			// to push one of the test possibilities to the stack to be
			// explored later). Else, we take note of the new edge found
			// and, since we have just found a loop, we backtrack by
			// throwing a LoopFoundException.
			if (!exploredNodes.containsKey(fullName)) {
				Outcome nextOutcome = action.get(); // Carry on!
				
				// Put next outcome to the current list:
				decisionList.add(nextOutcome);
				listIndex++;
				
				return Outcome.effectOf(nextOutcome, noSend, noReceive);
			} else {
				Node node = exploredNodes.get(fullName);

				// Put edge in flowchart:
				flowchart.putEdge(lastNode, node, decisionList
						.get(decisionList.size() - 1));
				
				// Put edge in causality:
				causality.addExtraEdge(node);
				
				// The current node becomes the last node (if you get me...)
				lastNode = node;

				// Interrupt execution:
				throw new LoopFoundException();
			}
		} else {
			return Outcome.effectOf(decisionList.get(listIndex++), noSend,
					noReceive);
		}
	}
	
	/**
	 * Inscribes a node to be visited later in the search with a different
	 * outcome than the one passed to
	 * {@link #onSearch(Object, Supplier, TxException, RxException)}.
	 * This is used as the mechanism to put the yet unexplored adjacencies of
	 * the node in the search.
	 * 
	 * @param node
	 *            the current node
	 * @param futureOutcome
	 *            the yet unexplored outcome for that node.
	 * @param newTips
	 *            the set of tips in the Causality graph to be used in the
	 *            context to be explored.
	 * @see #onSearch(Object, Supplier, TxException, RxException)
	 */
	private void leaveForLater(Node node, Outcome futureOutcome,
			Map<String, Tip> newTips) {
		// Clone decision list:
		ArrayList<Outcome> newDecisionList =
				new ArrayList<Outcome>(decisionList);
		
		// Add future outcome to cloned list (= new list):
		newDecisionList.add(futureOutcome);
		
		// Push new decision list to decision list stack:
		searchStack.push(newDecisionList);
		
		// Push node to node stack:
		lastNodeStack.push(node);
		
		// Push new tips to the stack:
		tipsStack.push(newTips);
	}
	
	/**
	 * Declares a new node to the search. This inscribes the node in the
	 * explored set and gives it a fullName, which distinguishes it from other
	 * nodes.
	 * 
	 * @param node
	 *            the new node found in the search.
	 * @param fullName
	 *            a unique identifier for this node.
	 * @see #onSearch(Object, Supplier, TxException, RxException)
	 */
	private void declareNode(Node node, Object fullName) {
		// Set as explored:
		exploredNodes.put(fullName, node);

		// Put edge in flowchart:
		flowchart.putEdge(lastNode, node, decisionList
				.get(decisionList.size() - 1));
		
		// The current node becomes the last node (if you get me...)
		lastNode = node;
	}
	
	/**
	 * Enacts another play translating the character names with the map
	 * structure given. An optional parameter, prefix, is given to avid
	 * potential namespace clashes. It is recommended not to omit it.
	 * 
	 * Please note that the cast must be valid. That is, the actors in this play
	 * should be castable to the actors associated in the play to be enacted.
	 * 
	 * (Warning: not debugged yet).
	 * 
	 * @param play
	 *            the play to be enacted.
	 * @param cast
	 *            a valid translation from characters in the other play to
	 *            characters in this play.
	 * @param prefix
	 *            a prefix to be applied to all messages in the play to be
	 *            enacted.
	 */
	protected final void enact(Play<P> play, Map<String, String> cast,
			String prefix) {
		Pair<String, String> fullName = new Pair<String, String>(
				play.protocolName, prefix);

		onSearch(fullName, () -> {
			// First, test if cast is valid:
			cast.entrySet().forEach(entry -> {
				Class<?> thisActorClass = this.actorClasses.get(entry.getValue());
				Class<?> otherActorClass = play.actorClasses.get(entry.getKey());

				if (otherActorClass.isAssignableFrom(thisActorClass)) { // ????
					throw new RuntimeException(String.format(
							"Cast %s for %s encating %s is invalid. " +
							"Check classes for each character in both plays.",
							cast, this.getProtocolName(), play.getProtocolName()));
				}
			});
			
			EnactNode node = new EnactNode(play.getFlowchart(), cast, prefix);

			// Explore the exceptional cases later:
			//leaveForLater(node, Outcome.NO_SEND);
			//leaveForLater(node, Outcome.NO_RECEIVE);
			declareNode(node, fullName);

			return Outcome.OK;
		}, null, null); // TODO put something instead of null.
	}
	
	/**
	 * Implements a representation of a character in a play. A character can
	 * send things, run things and test things.
	 * 
	 * @author rongomai
	 *
	 * @param <A>
	 *            the Actor class to run the role described by this character.
	 *            The actor stores the local state of the protocol and gives
	 *            access to the runtime environment.
	 * @see Actor
	 */
	protected class Character<A extends Actor<P>> {
		
		private final String characterName;
		private final TxException noSend;
		private final RxException noReceive;
		
		/**
		 * Creates a new character in the play specifying custom instances of
		 * the types of TxException and RxException that this character
		 * catches. In case either is not specified, the character will respond
		 * to the super-classes NoTxException and RxException
		 * respectively.
		 * 
		 * @param characterActorClass
		 *            the class object of the Actor class given in the template
		 *            parameter of this class. This is necessary because of
		 *            Java's irritating type erasure mechanism.
		 * @param characterName
		 *            an intuitive name for the character (like "alice", "bob"
		 *            or "server") to be used as the character's identifier with
		 *            {@link Play#interpretAs(String)}. It must be unique in the
		 *            play.
		 * @param noSend
		 *            a TxException of the type that this character catches.
		 * @param noReceive
		 *            a RxException of the type this character catches.
		 */
		public Character(Class<?> characterActorClass, String characterName,
				TxException noSend, RxException noReceive) {
			this.characterName = characterName;
			this.noSend = noSend;
			this.noReceive = noReceive;
			
			// Test classes:
			if (!Actor.class.isAssignableFrom(characterActorClass)) {
				throw new RuntimeException("Supplied actor class must " + 
						"extend Actor.");
			} else if (actorClasses.containsKey(characterName)) {
				throw new RuntimeException("Duplicate character indetifier.");
				// New feature coming! By now, this is not supported.
			} else {
				actorClasses.put(characterName, characterActorClass);
			}
			
			// Register in the causality graph for a tip:
			causality.putCharacter(characterName);
		}
		
		/**
		 * Creates a character given an identifier, assuming it will catch
		 * ReceiveException as receiver-side exception.
		 * 
		 * @see #Constructor(Class, String, TxException, RxException)
		 * 
		 */
		public Character(Class<?> characterActorClass, String characterName,
				TxException noSend) {
			this(characterActorClass, characterName, noSend,
					new RxException());
		}
		
		/**
		 * Creates a character given an identifier, assuming it will catch
		 * NoTxException as sender-side exception.
		 * 
		 * @see #Character(Class, String, TxException,
		 *      RxException)
		 */
		public Character(Class<?> characterActorClass, String characterName,
				RxException noReceive) {
			this(characterActorClass, characterName, new TxException(),
					noReceive);
		}

		/**
		 * Creates a character given an identifier, assuming it will catch
		 * NoTxException as sender-side exception and RxException as
		 * receiver-side exception.
		 * 
		 * @see Character#Constructor(Class, String, TxException, RxException)
		 */
		public Character(Class<?> characterActorClass, String characterName) {
			this(characterActorClass, characterName, new TxException(),
					new RxException());
		}

		/**
		 * Makes the character run a set of actions.
		 * 
		 * @param run
		 *            a lambda expression defining the actions to be run.
		 * @see #run(RunnableWithActor, String)
		 */
		public void run(RunnableWithActor<A> run) {
			run(run, run.getClass().getName());
		}
		
		/**
		 * Makes the character run a set of actions.
		 * 
		 * @param run
		 *            a lambda expression defining the actions to be run.
		 * @param runName
		 *            a name to identify the run. This should be used when
		 *            passing the same lambda expression (same reference) twice
		 *            in the Play scene. The name avoids both calls to run from
		 *            being indistinguishable.
		 * @see #run(RunnableWithActor)
		 */
		public void run(RunnableWithActor<A> run, String runName) {
			Pair<String, String> fullName = new Pair<String, String>(
					characterName, runName);

			onSearch(fullName, () -> {
				RunNode<?> node = new RunNode<A>(characterName, run, runName);

				Tip tip = causality.getTipSetNode(characterName, node);
				
				declareNode(node, fullName);

				causality.setTip(characterName, tip.branch());
				
				return Outcome.OK;
			}, noSend, noReceive);
		}

		/**
		 * Make the character run a set of actions whose outcome is a boolean.
		 * This method is to be used inside flux control structures, such as
		 * whiles and ifs.
		 * 
		 * @param test
		 *            the test to be performed.
		 * @return the value of the test (it's actually more complicated than
		 *         that, but never mind).
		 */
		public boolean test(TestableWithActor<A> test) {
			return test(test, test.getClass().getName());
		}
		
		/**
		 * Make the character run a set of actions whose outcome is a boolean.
		 * This method is to be used inside flux control structures, such as
		 * whiles and ifs.
		 * 
		 * @param test
		 *            the test to be performed.
		 * @param testName
		 *            a name to identify the test. This should be used when
		 *            passing the same lambda expression (same reference) twice
		 *            in the Play scene. The name avoids both calls to test from
		 *            being indistinguishable.
		 * @return the value of the test (it's actually more complicated than
		 *         that, but never mind).
		 * @see #test(TestableWithActor)
		 */
		public boolean test(TestableWithActor<A> test,
				String testName) {
			Pair<String, String> fullName = new Pair<String, String>(
					characterName, testName);

			return onSearch(fullName, () -> {
				TestNode<?> node = new TestNode<A>(characterName, test, testName);

				Tip tip = causality.getTipSetNode(characterName, node);
				
				// Explore the else-clause later: push to search stack!
				Map<String, Tip> falseTips = causality.copyTips();
				falseTips.put(characterName, tip.branch());
				leaveForLater(node, Outcome.FALSE, falseTips);
				
				// Declare node, move tips and carry on:
				declareNode(node, fullName);
				
				causality.setTip(characterName, tip.branch());
				
				return Outcome.TRUE; // Carry on!
			}, noSend, noReceive);
		}
		
		/**
		 * Makes the character send a message to another character.
		 * 
		 * @param receiver
		 *            the receiving character.
		 * @param name
		 *            the message's name (e.g., "ack").
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, String, int)
		 */
		public void send(Character<?> receiver, String name)
				throws TxException, RxException {
			send(receiver, name, Integer.MAX_VALUE);
		}

		/**
		 * Makes the character send a message to another character. The content
		 * of the message is generated by onSend at the sender side and is
		 * processed by onReceive at the receiver side.
		 * 
		 * @param <B>
		 *            the actor type of the receiving character.
		 * @param receiver
		 *            the receiving character.
		 * @param onSend
		 *            a lambda expression taking a sending Actor object and
		 *            returning the content of the message to be sent.
		 * @param onReceive
		 *            a lambda expression taking a receiving Actor object and
		 *            the content of the message to be received.
		 * @param name
		 *            the name of the message.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, SupplierWithActor, String)
		 * @see #send(Character, SupplierWithActor, String, int)
		 * @see #send(Character, ConsumerWithActor, String)
		 * @see #send(Character, ConsumerWithActor, String, int)
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String,
		 *      int)
		 */
		public <B extends Actor<P>> void send(Character<B> receiver,
				SupplierWithActor<P, A> onSend,
				ConsumerWithActor<P, B> onReceive, String name) {
			send(receiver, onSend, onReceive, name, Integer.MAX_VALUE);
		}

		/**
		 * Makes the character send a message to another character. The content
		 * of the message is generated by onSend at the sender side.
		 * 
		 * @param receiver
		 *            the receiving character.
		 * @param onSend
		 *            a lambda expression taking a sending Actor object and
		 *            returning the content of the message to be sent.
		 * @param name
		 *            the name of the message.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, SupplierWithActor, String, int)
		 * @see #send(Character, ConsumerWithActor, String)
		 * @see #send(Character, ConsumerWithActor, String, int)
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String)
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String,
		 *      int)
		 */
		public void send(Character<?> receiver, SupplierWithActor<P, A> onSend,
				String name) {
			send(receiver, onSend, name, Integer.MAX_VALUE);
		}

		/**
		 * Makes the character send a message to another character. The content
		 * of the message is processed by onReceived at the receiver side.
		 * 
		 * @param <B>
		 *            the Actor type of the receiving character.
		 * @param receiver
		 *            the receiving character.
		 * @param onReceive
		 *            a lambda expression taking a receiving Actor object and
		 *            the content of the message to be received.
		 * @param name
		 *            the name of the message.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, SupplierWithActor, String)
		 * @see #send(Character, SupplierWithActor, String, int)
		 * @see #send(Character, ConsumerWithActor, String, int)
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String)
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String,
		 *      int)
		 */
		public <B extends Actor<P>> void send(Character<B> receiver,
				ConsumerWithActor<P, B> onReceive, String name) {
			send(receiver, onReceive, name, Integer.MAX_VALUE);
		}

		/**
		 * Same as {@link #send(Character, String)}, but allows specifying a
		 * timeout, after which the message will no longer be received (a
		 * TxException might be raised if the character is unable to proceed
		 * without this message).
		 * 
		 * @param receiver
		 *            the receiving character's character object.
		 * @param name
		 *            the message's name (e.g., "ack").
		 * @param timeout
		 *            the timeout, expressed in milliseconds.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, String)
		 */
		public void send(Character<?> receiver, String name, int timeout)
				throws TxException, RxException {
			Pair<String, String> fullName = new Pair<String, String>(name, 
					receiver.characterName);
			
			onSearch(fullName, () -> {
				SendNode node = new SendNode(characterName,
						receiver.characterName, name, timeout);
				
				// Retrieve current tips (and branch the receiver's):
				Tip senderTip = causality.getTipSetNode(characterName, node);
				Tip receiverTip = causality.getTip(receiver.characterName).branch();
				
				// "Cause" the message:
				senderTip.cause(receiverTip, timeout);
				
				// Create new tips:
				Tip senderOk = senderTip.branch();
				Tip senderNoSend = senderTip.branch();
				Tip receiverOk = receiverTip.branch();
				Tip receiverNoReceive = receiverTip.branch();
				
				// Explore the exceptional cases later:
				Map<String, Tip> noSendTips = causality.copyTips();
				noSendTips.put(characterName, senderNoSend);
				noSendTips.put(receiver.characterName, receiverTip);
				leaveForLater(node, Outcome.NO_SEND, noSendTips);
				
				Map<String, Tip> noReceiveTips = causality.copyTips();
				noReceiveTips.put(characterName, senderOk);
				noReceiveTips.put(receiver.characterName, receiverNoReceive);
				leaveForLater(node, Outcome.NO_RECEIVE, noReceiveTips);
				
				// Declare node, move tips and carry on:
				declareNode(node, fullName);
				
				causality.setTip(characterName, senderOk);
				causality.setTip(receiver.characterName, receiverOk);
				
				return Outcome.OK;
			}, this.noSend, receiver.noReceive);
		}
		
		/**
		 * Same as
		 * {@link #send(Character, SupplierWithActor, ConsumerWithActor, String, int)}
		 * , timeout, after which the message will no longer be received (a
		 * TxException might be raised if the character is unable to proceed
		 * without this message).
		 * 
		 * @param <B>
		 *            the Actor type of the receiving character.
		 * @param receiver
		 *            the receiving character.
		 * @param onSend
		 *            a lambda expression taking a sending Actor object and
		 *            returning the content of the message to be sent.
		 * @param onReceive
		 *            a lambda expression taking a receiving Actor object and
		 *            the content of the message to be received.
		 * @param name
		 *            the name of the message.
		 * @param timeout
		 *            the timeout expressed in milliseconds.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, SupplierWithActor, ConsumerWithActor, String)
		 */
		public <B extends Actor<P>> void send(Character<B> receiver,
				SupplierWithActor<P, A> onSend,
				ConsumerWithActor<P, B> onReceive, String name, int timeout) {
			this.run(me -> me.setMessage(name, onSend.get(me)), "on sending "
					+ name + " to " + receiver.characterName);
			this.send(receiver, name, timeout);
			receiver.run(me -> onReceive.accept(me, me.getMessage(name)),
					"on receiving " + name + " from " + this.characterName);
		}
		
		/**
		 * Same as {@link #send(Character, SupplierWithActor, String)}, but
		 * allows specifying a timeout, after which the message will no longer
		 * be received (a TxException might be raised if the character is
		 * unable to proceed without this message).
		 * 
		 * @param receiver
		 *            the receiving character.
		 * @param onSend
		 *            a lambda expression taking a sending Actor object and
		 *            returning the content of the message to be sent.
		 * @param name
		 *            the name of the message.
		 * @param timeout the timeout expressed in milliseconds.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, SupplierWithActor, String)
		 */
		public void send(Character<?> receiver, SupplierWithActor<P, A> onSend,
				String name, int timeout) {
			this.run(me -> me.setMessage(name, onSend.get(me)), "on sending "
					+ name + " to " + receiver.characterName);
			this.send(receiver, name, timeout);
		}
		
		/**
		 * Same as {@link #send(Character, ConsumerWithActor, String)}, timeout,
		 * after which the message will no longer be received (a TxException
		 * might be raised if the character is unable to proceed without this
		 * message).
		 * 
		 * @param <B>
		 *            the Actor type of the receiving character.
		 * @param receiver
		 *            the receiving character.
		 * @param onReceive
		 *            a lambda expression taking a receiving Actor object and
		 *            the content of the message to be received.
		 * @param name
		 *            the name of the message.
		 * @param timeout
		 *            the timeout expressed in milliseconds.
		 * @throws TxException
		 *             in case message cannot be sent by any reason.
		 * @throws RxException
		 *             in case the sender discovers the message was not received
		 *             by any reason.
		 * @see #send(Character, ConsumerWithActor, String)
		 */
		public <B extends Actor<P>> void send(Character<B> receiver,
				ConsumerWithActor<P, B> onReceive, String name, int timeout) {
			this.send(receiver, name, timeout);
			receiver.run(me -> onReceive.accept(me, me.getMessage(name)),
					"on receiving " + name + " from " + this.characterName);
		}
		
		/**
		 * Makes the character query another character. The queried character
		 * receives the query messages, processes it and sends a response back
		 * to the querier.
		 * 
		 * @param <B>
		 *            the Actor type of the queried character.
		 * @param receiver
		 *            the queried character.
		 * @param process
		 *            a lambda expression taking a queried Actor object and the
		 *            query message content and returning the response message
		 *            content.
		 * @param queryName
		 *            the name of the query.
		 */
		public <B extends Actor<P>> void query(Character<B> receiver,
				UnaryWithActor<P, B> process, String queryName) {
			this.send(receiver, queryName + "Query");
			
			receiver.run(
					(me) -> {
						me.setMessage(
								queryName + "Response",
								process.get(me,
										me.getMessage(queryName + "Query")));
					}, /* process.getClass().getName() + */"on querying "
							+ queryName);
			
			receiver.send(this, queryName + "Response");
		}
	}
	
	/**
	 * A character that uses Actor directly, i.e., has no internal state.
	 * 
	 * @author rongomai
	 * 
	 * @see Character
	 * @see Actor
	 */
	protected final class StatelessCharacter extends Character<Actor<P>> {
		public StatelessCharacter(String characterName, TxException noSend,
				RxException noReceive) {
			super(Actor.class, characterName, noSend, noReceive);
		}
		
		public StatelessCharacter(String characterName, TxException noSend) {
			super(Actor.class, characterName, noSend);
		}
		
		public StatelessCharacter(String characterName,
				RxException noReceive) {
			super(Actor.class, characterName, noReceive);
		}
		
		public StatelessCharacter(String characterName) {
			super(Actor.class, characterName);
		}
	}
}