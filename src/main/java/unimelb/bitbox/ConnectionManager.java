package unimelb.bitbox;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.Err.JsonParserException;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ConnectionManager implements NetworkObserver {
	
	int MAX_NO_OF_CONNECTION;
	private static Logger log = Logger.getLogger(ConnectionManager.class.getName());
	//ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	Map<String, HostPort> activePeerHostPort;// In UDP Mode it will remember UDP peers HostPort as well
	
	// In UDP mode, the Mapping below is
	// Used to ensure is used for HandShake
	// SEND purposes. Whenever a HandShake
	// is initiated, UDPHandShakeReceiver stops
	// considering peers in it as unknown peer
	// temporarily to ensure that sending
	// HandShake request is not confused with
	// HandShake Request from unknown sources.
	Map<String, HostPort> temporaryUDPRememberedConnections; 
	
	// Used Post HandShake and when a connection
	// has been remembered in UDP case. Consumer
	// will keep listening and start consuming all
	// messages in activePeerHostPort Mapping, which
	// is valid for receive cases.
	// In SEND-message-with-TimeOut cases, it is
	// required that the consumption is halted
	// temporarily. And this Mapping below is used
	//to ensure a message can be sent
	//to a peer with time_out setting and that
	// it is not consumed by the consumer.
	// Messages from Peers in this list
	// are not consumed by Receiving Consumers. 
	Map<String,HostPort>  temporaryUDPSuspendedConsmers;
	Map<String, Connection> activePeerConnection;
	HostPort serverHostPort;
	private BlockingQueue<Message> incomingMessagesQueue;
	
	public static final String CONNECTION_MODE = Configuration.getConfigurationValue("mode").toLowerCase();
	
	public ConnectionManager(int maxNoOfConnections, HostPort serverHostPort, BlockingQueue<Message> MessageQueue)
	{
		this.MAX_NO_OF_CONNECTION = maxNoOfConnections;
		this.incomingMessagesQueue = MessageQueue;
		this.serverHostPort = serverHostPort;

		this.activePeerConnection = Collections.synchronizedMap(new HashMap<String, Connection>()); //Collections.synchronizedMap(new HashMap<>());
		this.activePeerHostPort= Collections.synchronizedMap(new HashMap<String, HostPort>());
		this.temporaryUDPRememberedConnections = Collections.synchronizedMap(new HashMap<String, HostPort>());
		this.temporaryUDPSuspendedConsmers = Collections.synchronizedMap(new HashMap<String, HostPort>());
	}
	
	public BlockingQueue<Message> getIncomingMessagesQueue() {
		return incomingMessagesQueue;
	}
	
	public void addConnection(Socket socket, PeerSource source, HostPort peerHostPort)
	{
		Connection connection;
		 
		if (socket!=null)
		{
			connection = new Connection(socket, this, source);
			
			activePeerConnection.put(connection.peer.toString(),connection);
			activePeerHostPort.put(connection.peer.toString(),peerHostPort);//valid for both TCP and UDP
			
			new Thread(connection).start();
			
			//After connection successfully added we need to start SyncEvents
			//This is not the ideal way to do it, but just easy to continue in the same manner as external events
			messageReceived(connection.peer,"{\"command\":\"SYNC_EVENTS\"}");
			
			
		}
	}
	

	
	public ArrayList<Document> getPeerList()
	{
		
		ArrayList<Document> peers = new ArrayList<Document>();
		for (HostPort peer: activePeerHostPort.values())
		{
			peers.add(peer.toDoc());
		}
		return peers;
	}


	public void sendAllPeers(Message msg) {
		String jsonMessage = msg.getJsonMessage();
		sendAllPeers(jsonMessage);
	}

	public void sendAllPeers(String msg) {
		String jsonMessage = msg;
		if (CONNECTION_MODE.toLowerCase().contentEquals("udp")) {
			//TODO GHD Fill
			if(activePeerHostPort.size() <= 0)
				log.warning("No Active connected Peers available to send message... Dropped");
			for (HostPort peer:activePeerHostPort.values())
			{
				try{
					log.info(String.format("Sending to %s, Message: %s",peer.toString(),jsonMessage));
					sendUDPPeerMsg(peer, jsonMessage, true);
				}
				catch(Exception e){
					log.warning("Unable to send message to peer "+peer.toString()+" message: "+jsonMessage);
				}
			}
		}			
		else {
			if(activePeerConnection.size()<=0)
				log.warning("No Active connected Peers available to send message... Dropped");
			for (Connection conn:activePeerConnection.values())
			{
				try{
					log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
					conn.send(jsonMessage, false);
				}
				catch(Exception e){
					log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
				}
			}
		}
	}
	
	public void sendToPeer(HostPort peer, Message msg, boolean terminateAfter, boolean isWithRetry) {
		String jsonMessage = msg.getJsonMessage();
		sendToPeer(peer, jsonMessage, terminateAfter,isWithRetry);
	}
	
	public void sendToPeer(HostPort peer, String jsonMessage, boolean terminateAfter, boolean isWithRetry) {
		if (CONNECTION_MODE.toLowerCase().contentEquals("udp")) {
			sendUDPPeerMsg(peer, jsonMessage, isWithRetry);
		}			
		else {
			//get the active connection to the specific peer
			Connection conn=activePeerConnection.get(peer.toString());
			if (conn!=null)
			{
				try{
					log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
					conn.send(jsonMessage, terminateAfter);
				}
				catch(Exception e){
					log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
				}
			}
			else {
				log.info(String.format("No active connection found with peer %s to send. could be dropped. Message: %s", peer.toString(), jsonMessage));
			}
		}
	}
	
	private void sendUDPPeerMsg(HostPort peer, String jsonMessage, boolean isWithRetry) {
		UDPSend udpSender = new UDPSend();
		//udpSender.sendBitBoxMessage(peer, jsonMessage);
		try{
			
			//UDPSend udpSender = new UDPSend();
			UDPReceive udpReceiver= new UDPReceive();
			int sendAttemptsCount = 0;
			boolean sent=false;

			// The line below will temporarily stop UDPHandShakeServer to consume
			// packets from UnknownSource pHostPort. This packet will now only
			// be consumed for Handshake consumers.
			if(!temporaryUDPSuspendedConsmers.containsKey(peer.toString())) {								
				temporaryUDPSuspendedConsmers.put(peer.toString(), peer);
				log.warning("Temporary suspend UDP consumer for send message to Peer:...."+peer.toString());
			}
			
			while (!sent && sendAttemptsCount < Constants.UDP_NUMBER_OF_RETRIES)
			{
				log.info(String.format("Attempt %s, Sending UDP to %s, Message: %s",sendAttemptsCount,peer.toString(),jsonMessage));
				udpSender.sendBitBoxMessage(peer, jsonMessage);
				
				if (isWithRetry) {
					String responseMsg= udpReceiver.receiveBitBoxMessage(peer, Constants.UDP_TIMEOUT);						
					//TODO Verify the message received if it is a response put back to queue otherwise
					
					log.info("UDPResponse Received Message-Response from peer " + peer.toString() + " is : "+ responseMsg);
					if (responseMsg != null) {
						if (responseMsg.equals("time_out")) { //retry to connect within connection attempts.
							sendAttemptsCount++;
							log.warning(String.format("TimeOut Received while waiting response. Attempting resend to peer=%s...attempt no %d", peer.toString(),
									sendAttemptsCount));
							}
							else {//Message is neither null, nor time out, so process it.
								messageReceived(peer, responseMsg);
								sent = true;
								// After confirming message sending, enable consumer process to get new messages
								temporaryUDPSuspendedConsmers.remove(peer.toString());
						} 
					}
				}
				else //Fire and forget so set sent status as true directly after first send attempt
				{
					sent = true;
					// After confirming message sending, enable consumer process to get new messages
					temporaryUDPSuspendedConsmers.remove(peer.toString());
				}
			}
			//if after retries the message is still lost, something is wrong with this peer we should terminate and consider it as closed connection
			if (!sent && sendAttemptsCount >= Constants.UDP_NUMBER_OF_RETRIES)
			{
				log.severe(String.format("Failed to send message after %s attempts, disconnecting peer %s",sendAttemptsCount,peer.toString()));
				UDPConnectionClosed(peer);
			}
		}
		catch(Exception e){
			log.warning("Unable to send message to peer "+peer.toString()+" message: "+jsonMessage);
		}
	}


	
	/** 
	 * To be called any time we need to forget the peer from active UDP, as there is no active connection concept
	 * @param connectionID
	 */
	public void UDPConnectionClosed(HostPort connectionID) {
		try {
			log.info("UDPConnectionClosed triggered. Removing UDP Peer from active connection list "+connectionID.toString());

			activePeerHostPort.remove(connectionID.toString());
			temporaryUDPSuspendedConsmers.remove(connectionID.toString());
			temporaryUDPRememberedConnections.remove(connectionID.toString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	public int getActiveConnectionCountBySource(PeerSource source) {
		int count = 0;
		for (Connection peer:activePeerConnection.values())
		{
			if (peer.peerSource == source)
				count++;
		}
		return count;
	}
	
	public boolean isPeerConnected(HostPort peer) {
		//get the active connection to the specific peer if exists
		boolean connected=false;
		synchronized (activePeerHostPort) {
			connected = activePeerHostPort.containsValue(peer);
		}
		return connected;
		/*
		//if would be better if we can verify the actual connection
		Connection conn = activePeerConnection.get(peer.toString());
		if (conn != null)
			return conn.isConnected();
		
		return false;
		*/
	}
	
	public int getActiveConnectionCount() {
		return activePeerHostPort.size();
	}
	
	public void rememberUDPConnection(HostPort peerHostPort)
	{	
		//TODO GHD to implement Connection logic
		//Connection connection;
		//connection = new Connection(socket, this, source);
			
			//activePeerConnection.put(connection.peer.toString(),connection);
			activePeerHostPort.put(peerHostPort.toString(),peerHostPort);
			
			//if a peer is to be remembered permanently it should also be
			//added to temporaryUDPConnections list
			temporaryUDPRememberedConnections.put(peerHostPort.toString(),peerHostPort);
			
			//new Thread(connection).start();
			
			//After connection successfully added we need to start SyncEvents
			//This is not the ideal way to do it, but just easy to continue in the same manner as external events
			//messageReceived(connection.peer,"{\"command\":\"SYNC_EVENTS\"}");
			
			
		
	}
	public int getUDPRememberedConnectionCount() {
		return activePeerHostPort.size();
	}
	
	@Override
	public void processNetworkEvent(FileSystemEvent fileSystemEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * This would be triggered by a connection incase the socket close or other exception occur.
	 *  Since the connection thread would be no longer running at this stage, remove the connection from the Active list
	 */
	@Override
	public void connectionClosed(HostPort connectionID) {
		try {

			log.info("Removing Peer from active connection list "+connectionID.toString());
			
			activePeerConnection.remove(connectionID.toString());

			activePeerHostPort.remove(connectionID.toString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enqueues a message in InMessageQueue. This method will be invoked by
	 * Individual Connections as they received messages.
	 */
	@Override
	public void messageReceived(HostPort connectionID, String message) {
		Message inMsg;
			try {
				inMsg = new Message(message);
				inMsg.setFromAddress(connectionID);
				incomingMessagesQueue.put(inMsg);
				if(inMsg.getCommand()==Command.INVALID_PROTOCOL)
					log.severe("Got INVALID_PROTOCOL from "+connectionID.toString());
			}
			catch(JsonParserException e) {
				//Error during message parsing, return invalid protocol to sender
				log.severe("TE: messageReceived.catch.JsonParserException:"+message);
				String[] msg = new String[1];
				msg[0] = e.getMessage();
				sendToPeer(connectionID, Protocol.createMessage(Command.INVALID_PROTOCOL, msg), true, false);
			} 
			catch (InvalidCommandException e) {
				//Error during message parsing, return invalid protocol to sender
				String[] msg = new String[1];
				msg[0] = e.getMessage();
				log.severe("TE: messageReceived.catch.InvalidCommandException:"+message);
				sendToPeer(connectionID, Protocol.createMessage(Command.INVALID_PROTOCOL, msg), true, false);
				log.severe("Message parsing failed "+e.getMessage());
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			
	}
	/**
	 * Fulfils a Client's request to connect to a specific Peer
	 * @param peer: HostPort
	 * @return true if connection is established, false if not.
	 */
	public boolean connectPeer(HostPort peer)
	{
		boolean status=false;
		//TODO should have a switch for UDP and TCP
		//TCP code:
		if (this.activePeerHostPort.size() < this.MAX_NO_OF_CONNECTION) 
		{
			try
			{
				Socket socket = new Socket(peer.host,peer.port);
				if (socket!=null)
				{
					this.addConnection(socket, Constants.PeerSource.SERVER, peer);
					status = true;
				}
			} catch (UnknownHostException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			//TODO GHD WE SHOULD HANDEL THIS, return proper message
		}
		
		return status;
	}
	
	/**
	 * Fulfils a Client's request to disconnect to a specific Peer
	 * @param peer: HostPort
	 * @return true if connection is established, false if not.
	 */
	public boolean disconnectPeer(HostPort peer)
	{
		boolean status=false;
		//TODO adding code to disconnect a peer
		//TCP code:
		String peerKey=null;
		
		for (String p: this.activePeerHostPort.keySet())
		{
			if (this.activePeerHostPort.get(p).equals(peer)) peerKey=p;
		}

		if (peerKey!=null)
		{
			if(Peer.mode.equals("tcp")) {
				Connection c = this.activePeerConnection.get(peerKey);
				c.stop();
				this.activePeerConnection.remove(peerKey);
				this.activePeerHostPort.remove(peerKey);
				log.info(String.format("Peer %s has been disconnected based on client request",peer.toString()));
				status = true;
			}
			else if(Peer.mode.equals("udp")) {
				
				this.activePeerHostPort.remove(peerKey.toString());
				this.temporaryUDPRememberedConnections.remove(peerKey.toString());
				log.info(String.format("Peer %s has been un-remembered based on client request",peer.toString()));
				status = true;
			}
		}
		else {
			//TODO GHD Need to respond that peer was not found, not already connected or something!
		}
		
		return status;
	}
}
