package unimelb.bitbox;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;

public class ConnectionManager {
	int MAX_NO_OF_CONNECTION;
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	
	public ConnectionManager(int maxNoOfConnections)
	{
		this.MAX_NO_OF_CONNECTION = maxNoOfConnections;
	}
	
	public void addConnection(Socket socket)
	{
		if (socket!=null)
		{
			new Connection(socket);
		}
		
	}
	
	public ArrayList<String> getPeerList()
	{
		ArrayList<String> peerList = new ArrayList<String>();
		
		for (Connection peer: connectedPeers)
		{
			peerList.add(peer.getPeerName());
		}
		
		return peerList;
	}

	
	/**
	 * Represents an active incoming establish connection with a peer.
	 * @author Tarek Elbeik
	 *
	 */
	class Connection extends Thread
	{
		DataInputStream in;
		DataOutputStream out;
		Socket socket;
		public Connection(Socket socket)
		{
			this.socket = socket;
			try
			{
				in = new DataInputStream(
						new DataInputStream(this.socket.getInputStream()));
				out = new DataOutputStream(
						new DataOutputStream(this.socket.getOutputStream()));
				this.start();
			}
			catch (Exception e)
			{
				log.severe(e.getMessage());
			}
		}
		
		/**
		 * Retrieves connected peer's host name.
		 * @return
		 */
		public String getHostName()
		{
			String name=null;
			try
			{
				name = this.socket.getInetAddress().getHostName();
			}
			catch (Exception e)
			{
				log.severe(e.getMessage());
			}
			
			return name;		
		}
		
		/**
		 * Retrieves connected peer's port number.
		 * @return
		 */
		public int getPortNumber()
		{
			int port=-1;
			try
			{
				port = this.socket.getPort();
			}
			catch (Exception e)
			{
				log.severe(e.getMessage());
			}
			
			return port;		
		}
		
		/**
		 * Constructs peer name and port number separated by ":"
		 * @return
		 */
		public String getPeerName()
		{
			return this.getHostName()+":"+this.getPortNumber();
		}
		
		
		@Override
		public void run()
		{
			try
			{
				System.out.println("Starting protocol..");
				if (connectedPeers.size()<MAX_NO_OF_CONNECTION)
				{
					connectedPeers.add(new Connection(socket));
				}
				else
				{
					try
					{
						String msg = "";
						
						this.out.writeUTF(msg);
						
					}
					catch (Exception e)
					{
						
					}
				}
				
				Document receivedMsg = Document.parse(in.readUTF());
				
				if (Protocol.validate(receivedMsg))
				{
					//out.writeUTF("");
					int msgCounter=0;
					while (true)
					{
						String dataIn = in.readUTF();
						System.out.println("Server received: "+dataIn);
						System.out.println("Server writing started..");
						out.writeUTF("ACK ("+dataIn+msgCounter+")");	
					}
				}
				else
				{
					out.writeUTF(Protocol.INVALID_PROTOCOL);
					log.warning("Invalid protocol received, closing connection..");
					this.socket.close();
				}
				//receivedMsg.toJson
			}
			
			catch (SocketException e)
			{
				log.severe(String.format("Connection to %s has been lost", this.getPeerName()));
				System.out.println("Connection to "+socket.getInetAddress().getHostName()+" has been lost");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			finally
			{
				try
				{
					if (this.socket!=null) socket.close();
					if (this.in!=null) in.close();
					if (this.out!=null) out.close();
						
				}
				catch (Exception e)
				{
					log.severe(e.getMessage());
				}
			}
		}
	}

}
