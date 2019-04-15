package unimelb.bitbox;
import java.net.*;
import java.io.*;
import java.util.concurrent.BlockingQueue;
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
	BlockingQueue<Message> incomingMessagesQueue;    
	private volatile boolean running = true;

	public Connection(Socket clientSocket, BlockingQueue<Message> MessageQueue, ProcessMessage connectionManager) 
	{
		log = Logger.getLogger(Connection.class.getName());
		try
		{
			this.in = new DataInputStream(clientSocket.getInputStream());
			this.out = new DataOutputStream(clientSocket.getOutputStream());
			this.clientSocket = clientSocket;
			this.connectionManager = connectionManager;
			this.incomingMessagesQueue = MessageQueue;
			//connection ip/port
			this.peer = new HostPort(clientSocket.getInetAddress().getHostAddress(), clientSocket.getLocalPort());
			log.info("Active Connection maintained for: "+peer.toString());
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
				
	}
	
	public void stop() {
        running = false;
    }
	
	
	@Override
	public void run()
	{
		try
		{
			while (running)
			{
				if (this.outBuffer!="") {
					log.info("outBuffer content to send: "+outBuffer);
					out.writeUTF(outBuffer);
				}
				receive();
				if (!(inBuffer==null))
				{
					log.info("inBuffer content received: "+inBuffer);
					Message inMsg = new Message(inBuffer);
					inMsg.setFromAddress(peer);
					incomingMessagesQueue.put(inMsg); //add to inQueue that is used by Event Processor
					//this.connectionManager.enqueueMessage(inBuffer);
				}
				else
					System.out.println("In buffer is null");
			}
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
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
				Document host = (Document) handshakeRequest.get("hostPort");
				peer = new HostPort(host);
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
		log.info("in connection.send, message: "+message);
		this.outBuffer=message;
	}
	
	public String receive()
	{
		try
		{
			//TODO: this needs to be revised, the read is blocking, thus no outgoing message will be sent while this is waiting for incoming messages
			log.info("blocked at in.readUTF()");
			inBuffer = this.in.readUTF();
			log.info("after at in.readUTF()");
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
