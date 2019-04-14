package unimelb.bitbox;

import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;

public class ConnectionManager implements ProcessMessage {
	int MAX_NO_OF_CONNECTION;
	private static Logger log = Logger.getLogger(ConnectionManager.class.getName());
	//ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	Map<String, Connection> connectedPeers;
	HostPort serverHostPort;
	private BlockingQueue<Message> incomingMessagesQueue;

	public ConnectionManager(int maxNoOfConnections, HostPort serverHostPort, BlockingQueue<Message> MessageQueue)
	{
		this.MAX_NO_OF_CONNECTION = maxNoOfConnections;
		this.incomingMessagesQueue = MessageQueue;
		this.serverHostPort = serverHostPort;
		this.connectedPeers = new HashMap<String, Connection>(); //Collections.synchronizedMap(new HashMap<>());
	}
	
	public BlockingQueue<Message> getIncomingMessagesQueue() {
		return incomingMessagesQueue;
	}
	
	public void addConnection(Socket socket)
	{
		Connection connection;
		 
		if (socket!=null)
		{
			connection = new Connection(socket, incomingMessagesQueue, this);
			if (connection.isHSRReceived())
			{
				if (connectedPeers.size()<MAX_NO_OF_CONNECTION)
				{
					String[] message = new String[1];
					
					message[0] = this.serverHostPort.toDoc().toJson();
					connection.send(Protocol.createMessage(Constants.Command.HANDSHAKE_RESPONSE, message));
					new Thread(connection).start();
					connectedPeers.put(connection.peer.toString(),connection);
				}
				else
				{
					connection.send(getPeerList().toJson());
				}
				
			}
			else
			{
				String[] message = new String[1];
				message[0] = "Invalid protocol";
				connection.send(Protocol.createMessage(Constants.Command.INVALID_PROTOCOL, message));
				connection.close();
				
			}
		}
	}
	
	public Document getPeerList()
	{
		Document peerList = new Document();
		ArrayList<Document> peers = new ArrayList<Document>();
		for (Connection peer: connectedPeers.values())
		{
			peers.add(peer.peer.toDoc());
		}
		peerList.append("command", Constants.Command.CONNECTION_REFUSED.toString());
		peerList.append("message", "connection limit reached");
		peerList.append("peers", peerList);
		
		return peerList;
	}

	
	public void sendAllPeers(Message msg) {
		String jsonMessage = msg.getJsonMessage();
		for (Connection conn:connectedPeers.values())
		{
			try{
				log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
				conn.send(jsonMessage);
			}
			catch(Exception e){
				log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
			}
		}
	}
	
	public void sendToPeer(HostPort peer, Message msg) {
		//get the active connection to the specific peer
		Connection conn=connectedPeers.get(peer.toString());
		String jsonMessage = msg.getJsonMessage();
		try{
			log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
			conn.send(jsonMessage);
		}
		catch(Exception e){
			log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
		}
	}
	
	/**
	 * Enqueues a message in InMessageQueue. This method will be invoked by
	 * Individual Connections as they received messages.
	 */
	@Override
	public void enqueueMessage(String message)
	{
		System.out.println("Connection manager: enqueued message="+message);
		
	}
		
}
