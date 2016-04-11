package protocols;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import communications.FullMessage;
import communications.ShortMessage;
import communications.util.QueueResource;

public class UDPCommunication extends QueueResource<String> {

	public static final String protocolName = "udp";
	
	private DatagramSocket socket;
	private int bufferLength;

	public UDPCommunication(int port, int bufferLength) throws SocketException {
		this.socket = new DatagramSocket(port);
		this.bufferLength = bufferLength;
		
		start();
	}
	
	UDPCommunication(int port) throws SocketException {
		socket = new DatagramSocket(port);
		start();
	}
	
	public int getPort() {
		return socket.getLocalPort();
	}
	
	public int getBufferLength() {
		return bufferLength;
	}

	public void setBufferLength(int bufferLength) {
		this.bufferLength = bufferLength;
	}
	
	public void disconnect() {
		stop();
		socket.disconnect();
	}

	@Override public void sendMessage(ShortMessage<String> msg, String to) {
		byte[] data = (msg.getId() + "'" + msg.getName() + "'" + msg.getPayload()).getBytes();
		String[] addressSplit = to.split(":");
		
		DatagramPacket packet = null;
		
		try {
			packet = new DatagramPacket(data, data.length, InetAddress.getByName(addressSplit[0]), Integer.parseInt(addressSplit[1]));
		} catch (NumberFormatException | UnknownHostException e) {
			e.printStackTrace();
		}
		
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override public FullMessage<String> take() throws InterruptedException {
		byte[] buffer = new byte[bufferLength];
		DatagramPacket packet = new DatagramPacket(buffer, bufferLength);
		
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Ghaaa! Back to C-strings!!
		StringBuilder sb = new StringBuilder();
		
		for(byte b: packet.getData()) {
			if(b == 0)
				break;
			
			sb.append((char)b);
		}
		
		String[] split = new String(sb).split("'", 3);

		return new FullMessage<String>(
				Long.valueOf(split[0]), // id
				UDPCommunication.protocolName,
				split[1], // type
				packet.getSocketAddress().toString().substring(1), // sender address (without the bloody initial "/")
				split[2]); // payload
	}
}
