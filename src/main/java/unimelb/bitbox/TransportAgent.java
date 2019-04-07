package unimelb.bitbox;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class TransportAgent {
	
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	public void addConnection(Socket socket)
	{
		if (socket!=null)
		{
			this.connectedPeers.add(new Connection(socket));
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
				int msgCounter=0;
				while (true)
				{
					String dataIn = in.readUTF();
					System.out.println("Server received: "+dataIn);
					System.out.println("Server writing started..");
					out.writeUTF("ACK ("+dataIn+msgCounter+")");	
				}
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
