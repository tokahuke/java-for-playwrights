package xmpp;

import org.jivesoftware.smack.XMPPException;

import primitives.Query;
import protocols.XMPPWithIQProtocol;

public class Bob implements Runnable {
	@Override public void run() {
		XMPPWithIQProtocol comm = null;
		
		try {
			comm = new XMPPWithIQProtocol("127.0.0.1", 5222, "bob", "bob");
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		new Query.Server<String>(comm, "hello",  (String str) -> str + ", hohoho!");
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		comm.logout();
		System.out.println("Server ended!");
	}
}
