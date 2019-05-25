package unimelb.bitbox;

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
			activePeerHostPort.put(connection.peer.toString(),peerHostPort);
			
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
	
	public void sendToPeer(HostPort peer, Message msg, boolean terminateAfter) {
		String jsonMessage = msg.getJsonMessage();
		sendToPeer(peer, jsonMessage, terminateAfter);
	}
	
	public void sendToPeer(HostPort peer, String jsonMessage, boolean terminateAfter) {
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
		return activePeerConnection.size();
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
				sendToPeer(connectionID, Protocol.createMessage(Command.INVALID_PROTOCOL, msg), true);
			} 
			catch (InvalidCommandException e) {
				//Error during message parsing, return invalid protocol to sender
				String[] msg = new String[1];
				msg[0] = e.getMessage();
				log.severe("TE: messageReceived.catch.InvalidCommandException:"+message);
				sendToPeer(connectionID, Protocol.createMessage(Command.INVALID_PROTOCOL, msg), true);
				log.severe("Message parsing failed "+e.getMessage());
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			
	}
		
}
