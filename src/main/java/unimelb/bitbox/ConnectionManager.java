package unimelb.bitbox;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;
import unimelb.bitbox.*;

public class ConnectionManager implements ProcessMessage {
	int MAX_NO_OF_CONNECTION;
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	HostPort serverHostPort;
	public ConnectionManager(int maxNoOfConnections, HostPort serverHostPort)
	{
		this.MAX_NO_OF_CONNECTION = maxNoOfConnections;
		this.serverHostPort = serverHostPort;
	}
	
	public void addConnection(Socket socket)
	{
		Connection connection;
		 
		if (socket!=null)
		{
			connection = new Connection(socket,this);
			if (connection.isHSRReceived())
			{
				if (connectedPeers.size()<MAX_NO_OF_CONNECTION)
				{
					String[] message = new String[1];
					
					message[0] = this.serverHostPort.toDoc().toJson();
					connection.send(Protocol.createMessage(Constants.Command.HANDSHAKE_RESPONSE, message));
					new Thread(connection).start();
					connectedPeers.add(connection);
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
		for (Connection peer: connectedPeers)
		{
			peers.add(peer.peer.toDoc());
		}
		peerList.append("command", Constants.Command.CONNECTION_REFUSED.toString());
		peerList.append("message", "connection limit reached");
		peerList.append("peers", peerList);
		
		return peerList;
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
