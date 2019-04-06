package unimelb.bitbox;

import java.net.*;
import java.io.*;
import java.util.*;

public class TransportAgent {
	
	ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	
	
	public void addConnection(Socket socket)
	{
		if (socket!=null)
		{
			this.connectedPeers.add(new Connection(socket));
		}
		
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
				System.out.println("[TE-ERROR]:"+e.getMessage());
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				System.out.println("[TE-Run]: server connection started listening..");
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
				System.out.println("Connection to "+socket.getInetAddress().getHostName()+" has been lost");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

}
