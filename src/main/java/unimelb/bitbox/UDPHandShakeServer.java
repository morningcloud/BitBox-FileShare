package unimelb.bitbox;

import java.net.InetAddress;
import java.util.logging.Logger;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.Constants.PeerSource;

/**
 * This class will keep watching the
 * udpPacketQueue in UDPPortListener for
 * messages from new UDP Peers and generate 
 * HandShake Response or HandShake Refused.
 * It will also add successfully HandShaked 
 * Peers to remembered peers list
 *
 */
public class UDPHandShakeServer implements Runnable {
	ConnectionManager connectionManager;
	UDPSend udpSend;
	UDPReceive udpReceive;
	private static Logger log = Logger.getLogger(UDPHandShakeServer.class.getName());
	HostPort serverHostPort;
	
	public UDPHandShakeServer(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		serverHostPort = new HostPort(Peer.serverName,Peer.udpServerPort);
		this.udpSend = new UDPSend();
		this.udpReceive = new UDPReceive();
		
		
	}

	@Override
	public void run() {
		InetAddress remoteIPAddress;
		Integer remotePort;
		HostPort connectingPeer;
		log.info("UDPHandShakeServer has started and searching for Unknown Datagrams in Queue.");
		
		while(true) {
			try {	
				//Document handshakeMsg = null;
				String inmsg = null;
				if (!UDPPortProducer.udpPacketsQueue.isEmpty()) {
						remoteIPAddress = UDPPortProducer.udpPacketsQueue.peek().getAddress();
						remotePort = UDPPortProducer.udpPacketsQueue.peek().getPort();
						connectingPeer = new HostPort(remoteIPAddress.toString(),remotePort);
						log.severe("Receiving from unknown host.....:" +remoteIPAddress+":"+remotePort);
						
						// The if statement below will keep consuming
						// packets from non-remembered sources. It is doing so					
						// because Datagrams from peers initiating handshake
						// request are from non-remembered peers.
						if (!connectionManager.temporaryUDPRememberedConnections.containsKey(connectingPeer.toString())) {
							log.info("UDPHandShakeServer has received a Datagram from Unknown Source from Queue.");
							log.info(String.format("UDPHandShakeServer has started UDPReceive to start forming"
									+ " message from bytes: Remote %s:%s.", remoteIPAddress,remotePort.toString()));
							inmsg = udpReceive.receiveBitBoxMessage(remoteIPAddress, remotePort, null);// second parameter in timeout. null means default.
							//System.out.println("Size Of inmsg is: " + inmsg.length());
							log.info("1. UDPHandShakeServer has received the compiled message: "+ inmsg);	
							
							
							String responsOfHandShakeRequest = Protocol.createUDPHandShakeResponse(inmsg, connectionManager);
							udpSend.sendBitBoxMessage(remoteIPAddress, remotePort, responsOfHandShakeRequest);
						}
						
						
	
				}					
			}
			catch (Exception e) {
				log.severe("General Exception in UDPHandShakeServer..... Continuing Listening");
				continue;
			}
		}
				
	}
	

}
		

