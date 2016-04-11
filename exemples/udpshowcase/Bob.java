package udpshowcase;

import java.net.SocketException;

import primitives.Query;
import protocols.UDPCommunication;

public class Bob implements Runnable {
	public int port;
	UDPCommunication comm = null;
	
	Bob() {
		try {
			comm = new UDPCommunication(0, 1024);
			port = comm.getPort();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Override public void run() {
		new Query.Server<String>(comm, "hello",  (String str) -> str + ", hohoho!");
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		comm.disconnect();
		System.out.println("Server ended!");
	}
}
