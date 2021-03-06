package tcp;

import java.io.IOException;

import communications.util.Dump;
import primitives.Query;
import protocols.TCPCommunication;

public class Bob implements Runnable {
	public int port;
	TCPCommunication comm = null;
	
	Bob() {
		try {
			comm = new TCPCommunication(0);
			port = comm.getPort();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Override public void run() {
		new Query.Server<String>(new Dump<String>(comm), "hello",  (String str) -> str + ", hohoho!");
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		comm.disconnect();
		System.out.println("Server ended!");
	}
}
