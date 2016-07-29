package dsl;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class Causality {
	private final Map<String, Tip> initialTips = new HashMap<String, Tip>();
	private Map<String, Tip> tips = null;
	private final Map<Node, Tip> tipForNode = new HashMap<Node, Tip>();
	private final Map<Tip, Node> nodeForTip = new HashMap<Tip, Node>();
	
	public void putCharacter(String name) {
		initialTips.put(name, new Tip(name));
	}
	
	public Tip getTip(String name) {
		return tips.get(name);
	}
	
	public Tip getTip(Node node) {
		return tipForNode.get(node);
	}
	
	public void setTip(String name, Tip tip) {
		tips.put(name, tip);
	}
	
	public Tip getTipSetNode(String name, Node node) {
		Tip tip = tips.get(name);
		tipForNode.put(node, tip);
		nodeForTip.put(tip, node);
		return tip;
	}
	
	public void addExtraEdge(Node node) {
		Tip tip = tips.get(node.getCharacter());
		Tip returnTip = tipForNode.get(node);
		
		tip.cause(returnTip, 0); // This is a hack.
	}
	
	public Map<String, Tip> copyTips() {
		return new HashMap<String, Tip>(tips);
	}
	
	public Map<String, Tip> copyInitialTips() {
		return new HashMap<String, Tip>(initialTips);
	}
	
	public void setTips(Map<String, Tip> tips) {
		this.tips = tips;
	}
	
	public Map<SendNode, Map<SendNode, Integer>> findMaxDelays(String name) {
		Map<SendNode, Map<SendNode, Integer>> maxDelays = 
				new HashMap<SendNode, Map<SendNode, Integer>>();
		
		// For every outgoing message belonging to the character:
		tipForNode.forEach((node, theTip) -> {
			if (node instanceof SendNode && node.getCharacter().equals(name)) {
				Map<Tip, Integer> tipDist = new HashMap<Tip, Integer>();
				Map<SendNode, Integer> nodeDist = new HashMap<SendNode, Integer>();
				Set<Tip> wanted = new HashSet<Tip>();
				
				// Do a search to find all tips connected to it:
				Tip.doSearch(theTip, tip -> !tip.isEmpty()
						&& !tip.belongsTo(name),
						tip -> {
							tipDist.put(tip, 0);
							
							Node sendNode = nodeForTip.get(tip);
							if (sendNode != null 
									&& sendNode instanceof SendNode
									&& ((SendNode) sendNode).getReceiver()
									.equals(name)) {
								wanted.add(tip);
							}
						});
				
				// Then, run Bellman-Ford on the component we found: 
				Tip.doBellmanFord(theTip, tipDist);
				
				// Build the relation between the outgoing and the incoming
				// messages:
				tipDist.forEach((tip, maxDelay) -> {
					if (wanted.contains(tip)) {
						SendNode sendNode = (SendNode) nodeForTip.get(tip);
						int totalDelay = Tip.addSat(maxDelay, sendNode.getTimeout());
						
						if (totalDelay != Long.MAX_VALUE) {
							nodeDist.put(sendNode, totalDelay);
						}
					}
				});
				
				// Put relation in the maxDelays map:
				maxDelays.put((SendNode) node, nodeDist);
			}
		});
		
		return maxDelays;
	}
	
	public Map<Node, Set<Node>> findFinals(
			Map<Node, Set<Node>> flowchartReverse,
			Map<SendNode, Map<SendNode, Integer>> maxDelays) {
		Map<Node, Set<Node>> finals = new HashMap<Node, Set<Node>>();
		Queue<Node> queue = new ArrayDeque<Node>();
		Map<Node, Set<Node>> inverseRelation = new HashMap<Node, Set<Node>>();
		
		// Calculate the inverse relation:
		maxDelays.forEach((outNode, causedMap) -> {
			causedMap.forEach((inNode, delay) -> {
				// Build inverse relation:
				if (inverseRelation.containsKey(inNode)) {
					inverseRelation.get(inNode).add(outNode);
				} else {
					Set<Node> adj = new HashSet<Node>();
					adj.add(outNode);
					inverseRelation.put(inNode, adj);
				}
			});
		});
		
		// Find final outgoing messages for each incoming message:
		inverseRelation.forEach((inNode, outNodes) -> {
			Set<Node> finalSet = new HashSet<Node>();
			Set<Node> explored = new HashSet<Node>();
			
			// Put all reverse adjs of inNode in queue:
			flowchartReverse.get(inNode).forEach(node -> queue.add(node));
			
			while (!queue.isEmpty()) {
				Node node = queue.remove();
				
				if (explored.contains(node)) {
					continue;
				} else {
					explored.add(node);
				}
				
				if (outNodes.contains(node)) {
					// Found a "last out-message"! And don't go further.
					finalSet.add(node);
				} else {
					// Nope. Enqueue!
					flowchartReverse.get(node).forEach(adj -> {
						queue.add(adj);
					});
				}
			}
			
			finals.put(inNode, finalSet);
		});
		
		return finals;
	}
}
