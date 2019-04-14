package unimelb.bitbox;
import java.net.*;
import java.io.*;
import java.util.logging.Logger;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.*;

public class Connection implements Runnable {
	
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	ProcessMessage connectionManager;
	String inBuffer;
	String outBuffer;
	Logger log;
	HostPort peer;
	public Connection(Socket clientSocket, ProcessMessage connectionManager) 
	{
		log = Logger.getLogger(Connection.class.getName());
		try
		{
			this.in = new DataInputStream(clientSocket.getInputStream());
			this.out = new DataOutputStream(clientSocket.getOutputStream());
			this.clientSocket = clientSocket;
			this.connectionManager = connectionManager;
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
				
	}
	
	
	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				if (this.outBuffer!="")
					out.writeUTF(outBuffer);
				
				receive();
				if (!(inBuffer==null))
				{
					this.connectionManager.enqueueMessage(inBuffer);
				}
				else
					System.out.println("In buffer is null");
			}
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
	}
	
	/**
	 * Fills connection outgoing buffer with data to be sent to 
	 * @param message
	 */
	
	public boolean isHSRReceived()
	{
		boolean status = false;
		try
		{
			
			Document handshakeRequest = Document.parse(in.readUTF());
			if (Protocol.validate(handshakeRequest))
			{
				status = true;
				peer = new HostPort(handshakeRequest.getString("hostPort"));
			}
				
			else
				status = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return status;
	}
	
	public void send(String message)
	{
		this.outBuffer=message;
	}
	
	public String receive()
	{
		try
		{
			inBuffer = this.in.readUTF();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return inBuffer;
	}
	
	public void close()
	{
		try
		{
			if (this.clientSocket!=null)this.clientSocket.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
	}

}
