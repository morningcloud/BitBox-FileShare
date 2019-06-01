package unimelb.bitbox;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
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
				if (UDPPortProducer.udpPacketsQueue.size() !=0) {
						boolean fromUnknownSource = false;
						try {
							DatagramPacket peekedDatagram =  UDPPortProducer.udpPacketsQueue.peek();
							remoteIPAddress = peekedDatagram.getAddress();						
							remotePort = peekedDatagram.getPort();
							
							connectingPeer = new HostPort(remoteIPAddress.getHostAddress(),remotePort);
							System.out.println("HandshakeServer has searched Queue and found host: " + connectingPeer);	
							
							fromUnknownSource = !connectionManager.temporaryUDPRememberedConnections.containsKey(connectingPeer.toString())
									&& !connectionManager.activePeerHostPort.containsKey(connectingPeer.toString());
							System.out.println("is Host From Unknown Source: " + fromUnknownSource);
						}catch (NullPointerException e) {
							log.severe(this.getClass()+": Null Pointer Exception while peeking queue....packet not for me.....nothing serious....continuing");
							continue;
						}
						// The if statement below will keep consuming
						// packets from non-remembered sources. It is doing so					
						// because Datagrams from peers initiating handshake
						// request are from non-remembered peers.
						if (fromUnknownSource) {
							log.info("UDPHandShakeServer has received a Datagram from Unknown Source from Queue." +connectingPeer.toString() );
							
							DatagramPacket receivedDatagram = UDPPortProducer.udpPacketsQueue.take();							
							String completeMessage = getMessageFromDatagram(receivedDatagram);
							completeMessage.replaceAll("\n", "");
							completeMessage.replaceAll("\r", "");
							
							inmsg = completeMessage;
							//System.out.println("Size Of inmsg is: " + inmsg.length());
							log.info("1. UDPHandShakeServer has received the message: "+ inmsg + " from unknown source "+ connectingPeer.toString());	
							
							
							String responsOfHandShakeRequest = Protocol.createUDPHandShakeResponse(inmsg, connectionManager);
							udpSend.sendBitBoxMessage(remoteIPAddress, remotePort, responsOfHandShakeRequest);
						}
						
						
	
				}					
			}
			
			catch (Exception e) {
				log.severe("General Exception in UDPHandShakeServer..... Continuing Listening: " +e);
				e.printStackTrace();
				continue;
			}
		}
				
	}
	
	private String getMessageFromDatagram(DatagramPacket receivedDatagram )
			  {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			String messageInThisBlock = null;
		
			int lengthOfMessage = receivedDatagram.getLength();				
			byte[] dataToReceive  =new byte[lengthOfMessage];
			//System.out.println("Length of blockToReceive before is :" + initialBlockToReceive.length);									
			dataToReceive = receivedDatagram.getData();
			byte[] blockToReceive = new byte[lengthOfMessage];
			
			for(int b=0; b<lengthOfMessage; b++) {
				blockToReceive[b] = dataToReceive[b];
			}
			
			baos.write(blockToReceive, 0, blockToReceive.length);
			try {
				messageInThisBlock = baos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.severe("Unsupported Encoding From Unknow Source");
				return null;
			}
		
				
		return messageInThisBlock;
		
	}
	

}
		

