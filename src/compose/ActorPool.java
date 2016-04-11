package compose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import dsl.Actor;

class ActorPool<A extends Actor<?>> {
	
	private final Supplier<A> actorFactory;
	private final int size;
	private final ArrayList<A> idle;
	private final Map<A, Long> active;
	private final Map<Long, A> activeReverse;
	
	private int created = 0;
	
	public ActorPool(Supplier<A> actorFactory, int size) {
		super();
		this.actorFactory = actorFactory;
		this.size = size;
		this.idle = new ArrayList<A>(size + 1);
		this.active = new HashMap<A, Long>();
		this.activeReverse = new HashMap<Long, A>();
	}
	
	public synchronized A getFreshActor(long id) {
		// If everybody is working, will need to find somebody else:
		if (idle.isEmpty()) { // But only if there is still room!
			if(created < size) {
				// Make an actor out of thin air:
				A newActor = actorFactory.get();
				
				// Set things up:
				newActor.reset();
				
				// Put it in the idle bunch:
				idle.add(newActor);
				
				// Give the lad a thread to work:
				buildThread(newActor).start();
				
				created++;
			} else { // If we cannot afford anybody else, wait!
				while(idle.isEmpty()) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return null;
					}
				}
			}
		} // Now, either way, we found the right chap for the job!
		
		A actor = idle.remove(0);
		
		// Two-way mapping between connection ID and actor:
		active.put(actor, id);
		activeReverse.put(id, actor);
		
		return actor;
	}
	
	public synchronized A getRunningActor(long id) {
		return activeReverse.get(id);
	}
	
	public synchronized boolean isRunningId(long id) {
		return activeReverse.containsKey(id);
	}
	
	private Thread buildThread(A actor) {
		return new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						// Wait for someone to call:
						synchronized (actor) {
							while (!isActive(actor)) {
								actor.wait();
							}
						}
						
						// Run it! NO reset, please!
						actor.performWithoutResetting();
						
						// NOW, we restart:
						actor.reset();
						
						// Done!
						synchronized (ActorPool.this) {
							// Undo two-way association:
							activeReverse.remove(active.remove(actor));
							
							// The actor is officially idle:
							idle.add(actor);
							
							// Spread the word!
							ActorPool.this.notify();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					synchronized (ActorPool.this) {
						// Undo two-way association:
						activeReverse.remove(active.remove(actor));
						
						// Pretend it never existed:
						created--;
					}
				}
			}
		});
	}
	
	private final synchronized boolean isActive(A actor) {
		return active.containsKey(actor);
	}
}
