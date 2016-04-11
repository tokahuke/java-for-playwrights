package dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

class Tip {
	/**
	 * An id counter. The ids are used to represent Tips as Strings.
	 * 
	 *  @see #dbgPrint()
	 */
	private static int i = 0;
	
	/**
	 * For printing components of the graph.
	 * 
	 * @see #dbgPrint()
	 */
	static HashSet<Tip> explored; // Quick and dirty.
	
	/**
	 * Pulls new ids to represent new tips.
	 * 
	 * @return a fresh new unique identifier.
	 */
	private static String getId() {
		return Integer.toString(++i);
	}
	
	/**
	 * Does a breadth-first search from a source node.
	 * 
	 * @param start
	 *            the source node.
	 * @param filter
	 *            a predicate including nodes in the search (except start).
	 * @param action
	 *            an action to be executed with each node found (including
	 *            start).
	 */
	public static void doSearch(Tip start, Predicate<Tip> filter,
			Consumer<Tip> action) {
		LinkedList<Tip> queue = new LinkedList<Tip>();
		Set<Tip> explored = new HashSet<Tip>();
		
		action.accept(start);
		
		start.adjs.keySet().forEach(adj -> {
			queue.add(adj);
			action.accept(adj);
		});
		
		while(!queue.isEmpty()) {
			Tip tip = queue.pollFirst();
			
			if (!explored.contains(tip) && filter.test(tip)) {
				explored.add(tip);
				
				tip.adjs.keySet().forEach(adj -> {
					queue.add(adj);
					action.accept(adj);
				});
			}
		}
	}
	
	/**
	 * Executes the Bellman-Ford algorithm to find longest paths from a source
	 * node.
	 * 
	 * @param start
	 *            the tip to start the search from.
	 * @param distances
	 *            an already populated map that stores maximum distances.
	 */
	public static void doBellmanFord(Tip start, Map<Tip, Integer> distances) {
		// Start is at distance 0 from start:
		distances.put(start, 0);
		
		// Trivial case: nothing to be done.
		if (distances.size() < 2) {
			return;
		}
		
		// Finding longest paths:
		for (int i = 0; i < distances.size() - 1; i++) {
			distances.forEach((tip, totalDelay) -> {
				tip.adjs.forEach((otherTip, delay) -> {
					int relaxed = addSat(totalDelay, delay); 
					
					// Inverse relaxation:
					if (relaxed > distances.getOrDefault(otherTip,
							Integer.MAX_VALUE)) {
						distances.put(otherTip, relaxed);
					}
				});
			});
		}
		
		// Search for loops:
		Set<Tip> loopTips = new HashSet<Tip>();
		
		distances.forEach((tip, totalDelay) -> {
			// If no loop linking to tip was yet found:
			if (!loopTips.contains(tip)) {
				// Try to see what edges inverse-relax, if any:
				tip.adjs.forEach((otherTip, delay) -> {
					long relaxed = addSat(totalDelay, delay);

					// If it relaxes, it's a loop (arbitrarily long paths
					// exist):
					if (relaxed > distances.getOrDefault(otherTip,
							Integer.MAX_VALUE)) {
						// All nodes reachable from otherTip are accessible by
						// arbitrarily long paths:
						doSearch(otherTip, aTip -> distances.keySet()
								.contains(aTip), aTip -> loopTips.add(aTip));
					}
				});
			}
		});
		
		// Consolidate loops:
		loopTips.forEach(tip -> {
			distances.put(tip, Integer.MAX_VALUE);
		});
	}
	
	/**
	 * Does saturating addition.
	 * 
	 * @param a
	 *            first number
	 * @param b
	 *            second number
	 * @return saturating addition result
	 */
	public static int addSat(int a, int b) {
		int sum = a + b;
		return sum > -1 ? sum : Integer.MAX_VALUE;
	}
	
	/**
	 * A unique identifier for the tip. Used in {@link #toString()}.
	 */
	private String id = getId();
	
	/**
	 * The name of the character that "owns" the tip.
	 */
	private String characterName;
	
	/**
	 * Adjacencies mapping. Indicates which tips come next and with what maximum
	 * delay.
	 */
	private Map<Tip, Integer> adjs = new HashMap<Tip, Integer>();

	/**
	 * Builds new tip for character name.
	 * 
	 * @param characterName
	 *            the name of the character "owning" the tip.
	 */
	Tip(String characterName) {
		// Constructor is package-private.
		this.characterName = characterName;
	}
	
	/**
	 * Builds a new tip branching directly from the current tip. The tip created
	 * is owned by the same character and the associated edge has delay zero.
	 * 
	 * @return a new tip owned by the same character.
	 * @see #branch(int)
	 */
	public Tip branch() {
		Tip tip = new Tip(characterName);
		adjs.put(tip, 0);
		
		return tip;
	}
	
	/**
	 * Builds a new tip branching directly from the current tip. The tip created
	 * is owned by the same character.
	 * 
	 * @param maxDelay
	 *            the maximum delay acceptable.
	 * @return a new tip owned by the same character.
	 * @see #branch()
	 */
	public Tip branch(int maxDelay) {
		Tip tip = new Tip(characterName);
		adjs.put(tip, maxDelay);
		
		return tip;
	}
	
	/**
	 * Builds an edge from one tip to any other. This models the latency of the
	 * underlying communication channel.
	 * 
	 * @param other
	 *            another tip.
	 * @param maxDelay
	 *            the maximum delay acceptable.
	 */
	public void cause(Tip other, int maxDelay) {
		adjs.put(other, maxDelay);
	}
	
	/**
	 * Checks if tip is a sink (i.e., has no outgoing edges).
	 *  
	 * @return true if tip is a sink.
	 */
	public boolean isEmpty() {
		return adjs.isEmpty();
	}
	
	/**
	 * Checks if the tip is owned by a certain character.
	 * 
	 * @param name
	 *            the character's name.
	 * @return true if the tip is indeed owned by the character.
	 */
	public boolean belongsTo(String name) {
		return characterName.equals(name);
	}
	
	/**
	 * Prints in DFS order the connected component of the current tip. For
	 * debugging purposes only.
	 */
	public void dbgPrint() {
		explored.add(this);
		for (Tip tip: adjs.keySet()) {
			System.out.println(this + " --> " + tip);
		}
		
		for (Tip tip: adjs.keySet()) {
			if(!explored.contains(tip)) {
				tip.dbgPrint();
			}
		}
	}
	
	@Override public String toString() {
		return id;
	}
}