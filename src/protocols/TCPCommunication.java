package protocols;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import communications.FullMessage;
import communications.ShortMessage;
import communications.util.QueueResource;


/**
 * Socket flooding, hurray!
 * 
 * @author sys
 *
 */
public class TCPCommunication extends QueueResource<String> {
	public static final String protocolName = "tcp";
	
	private ServerSocket serverSocket;
	
	private Map<String, Socket> sockets = new HashMap<String, Socket>();
	// private Map<String, SocketListener> listeners = new HashMap<String,
	// SocketListener>();
	private BlockingQueue<FullMessage<String>> queue =
			new ArrayBlockingQueue<FullMessage<String>>(32);
	
	public TCPCommunication(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		
		new Thread(() -> {
			try {
				while (true) {
					Socket socket = serverSocket.accept();
					
					String address = socket.getRemoteSocketAddress().toString()
							.substring(1);
					
					synchronized (sockets) {
						new Thread(new SocketListener(address, socket, sockets,
								queue)).start();
						sockets.put(address, socket);
					}
				}
			} catch (IOException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}).start();
		
		start();
	}
	
	public void disconnect() {
		stop();
		
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getPort() {
		return serverSocket.getLocalPort();
	}
	
	@Override public void sendMessage(ShortMessage<String> msg, String to) {
		Socket socket = null;
		
		// Find the right socket:
		if (sockets.containsKey(to)) {
			socket = sockets.get(to);
		} else {
			String[] addressSplit = to.split(":");
			
			try {
				socket = new Socket(addressSplit[0],
						Integer.parseInt(addressSplit[1]));
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
			
			synchronized (sockets) {
				new Thread(new SocketListener(to, socket, sockets, queue))
						.start();
				sockets.put(to, socket);
			}
		}
		
		DataOutputStream outToServer = null;
		
		// Send the stuff:
		try {
			outToServer = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			outToServer.writeBytes(msg.getId() + "'" + msg.getName() + "'"
					+ msg.getPayload() + "\n");
		} catch (IOException e) {
			synchronized (sockets) {
				sockets.remove(socket);
			}
		}
	}
	
	@Override public FullMessage<String> take() throws InterruptedException {
		return queue.take();
	}
}

class SocketListener implements Runnable {
	private Socket socket;
	private BlockingQueue<FullMessage<String>> queue;
	private final String senderIp;
	private Map<String, Socket> sockets;
	
	public SocketListener(String remoteAddress, Socket socket,
			Map<String, Socket> sockets,
			BlockingQueue<FullMessage<String>> queue) {
		this.socket = socket;
		this.sockets = sockets;
		this.queue = queue;
		this.senderIp = remoteAddress;
	}
	
	@Override public void run() {
		BufferedReader buffer = null;
		
		try {
			buffer = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
			interrupt();
		}
		
		while (true) {
			String[] split = null;
			
			try {
				String read = buffer.readLine();
				
				if (read == null) { // Conncetion closed.
					interrupt();
					break;
				}
				
				split = read.split("'", 3);
			} catch (IOException e1) {
				interrupt();
				break;
			}
			
			try {
				queue.put(new FullMessage<String>(Long.valueOf(split[0]),  // id
						TCPCommunication.protocolName,
						split[1],  // type
						senderIp,  // sender address
						split[2]));// payload
			} catch (InterruptedException e) {
				interrupt();
				break;
			}
		}
	}
	
	private void interrupt() {
		synchronized (sockets) {
			sockets.remove(socket);
		}
		
		Thread.currentThread().interrupt();
	}
}