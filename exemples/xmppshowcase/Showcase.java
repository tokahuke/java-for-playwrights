package xmppshowcase;


public class Showcase {
	public static void main(String[] args) {
		new Thread(new Alice(), "Alice").start();
		new Thread(new Bob(), "Bob").start();
	}
}
