package xmppshowcase;

import java.io.IOException;

import org.jivesoftware.smack.XMPPException;

import primitives.Query;
import protocols.XMPPWithIQProtocol;

public class Alice implements Runnable {
	@Override public void run() {
		XMPPWithIQProtocol comm = null;
 
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		try {
			comm =  new XMPPWithIQProtocol("127.0.0.1", 5222, "alice", "alice");
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		Query.Client<String> client = new Query.Client<String>(comm, "hello", "bob@lb414-10/root");
		
		System.out.println(client.query("Hello"));
		
		comm.logout();
	}
}
