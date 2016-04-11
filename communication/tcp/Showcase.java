package tcp;


public class Showcase {
	public static void main(String[] args) {
		Bob bob = new Bob();
		Alice alice = new Alice(bob.port);
		
		new Thread(alice, "Alice").start();
		new Thread(bob, "Bob").start();
	}
}
