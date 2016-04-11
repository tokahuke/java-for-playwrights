package protocols;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;

import communications.CommunicationResource;
import communications.ReceiveEvent;
import communications.TxException;
import communications.ShortMessage;

public class XMPPWithIQProtocol implements CommunicationResource<String> {
	private ConnectionConfiguration config;
	private XMPPConnection connection;
	private Map<ReceiveEvent<String>, PacketListener> mreToListener = 
			new HashMap<ReceiveEvent<String>, PacketListener>();
	
	public XMPPWithIQProtocol(String ipAddress, int port, String user,
			String password) throws XMPPException {
		
		// Connect:
		config = new ConnectionConfiguration(ipAddress, port);
		config.setSASLAuthenticationEnabled(false);
        config.setSecurityMode(SecurityMode.disabled);
        
        connection = new XMPPConnection(config);
        connection.connect();
        
        // Login:
        connection.login(user, password, "root");
        
        // Add IQ provider:
        ProviderManager.getInstance().addIQProvider("query", "iq:myOwn", new MyIQProvider());
	}
	
	@Override public void addReceiveEvent(ReceiveEvent<String> receiveEvent) {
		// Yep! Do nothing. And yep! They all have different hashCodes...
		PacketListener packetListener = (Packet packet) -> {}; 
			
		mreToListener.put(receiveEvent, packetListener);
		
		connection.addPacketListener(packetListener, (Packet packet) -> {
			try {
				return packet instanceof MyIQ
						&& receiveEvent.receives(((MyIQ) packet).toMessage());
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				return false;
			}
		});
	}

	@Override public void removeReceiveEvent(ReceiveEvent<String> receiveEvent) {
		connection.removePacketListener(mreToListener.get(receiveEvent));
	}

	@Override public void sendMessage(ShortMessage<String> msg, String to) throws TxException {

		MyIQ myIQ = new MyIQ(msg.getName(), msg.getPayload());
		
		myIQ.setPacketID(String.valueOf(msg.getId()));
		myIQ.setTo(to);
		myIQ.setType(Type.SET); // We take care of that. So... dummy type.
		
		//Debug:
		//System.out.println(Thread.currentThread().getName()+" sending... " + myIQ.getTo());
		
		connection.sendPacket(myIQ);
	}

	public void logout() {
		connection.disconnect();
	}
}