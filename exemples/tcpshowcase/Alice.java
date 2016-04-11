package tcpshowcase;

import java.io.IOException;

import primitives.Query;
import protocols.TCPCommunication;


public class Alice implements Runnable {
	private int bobPort;
	TCPCommunication comm = null;
	
	Alice(int bobPort) {
		this.bobPort = bobPort;
		
		try {
			comm =  new TCPCommunication(0);
		} catch (IOException e) {
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
