package udpshowcase;

import java.net.SocketException;

import primitives.Query;
import protocols.UDPCommunication;


public class Alice implements Runnable {
	private int bobPort;
	UDPCommunication comm = null;
	
	Alice(int bobPort) {
		this.bobPort = bobPort;
		
		try {
			comm =  new UDPCommunication(0, 1024);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	@Override public void run() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Query.Client<String> client = new Query.Client<String>(comm, "hello", "127.0.0.1:"+Integer.toString(bobPort));
		
		System.out.println(client.query("Hello"));
		
		comm.disconnect();
	}
}
