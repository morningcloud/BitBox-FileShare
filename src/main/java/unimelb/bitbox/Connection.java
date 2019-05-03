package unimelb.bitbox;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.Constants.*;

public class Connection implements Runnable {
	
	BufferedReader in;
	BufferedWriter out;
	Socket clientSocket;
	NetworkObserver networkObserver;
	//Seems risky to define it at class level
	//String inBuffer;
	PeerSource peerSource;
	//GHD: changed outBuffer to list to cater for multiple parallel send requests
	ArrayList<OutBuffer> outBuffer;
	//String outBuffer;
	Logger log;
	HostPort peer;   
	private volatile boolean running = true;

	public Connection(Socket clientSocket, NetworkObserver connectionManager, PeerSource source) 
	{
		log = Logger.getLogger(Connection.class.getName());
		try
		{
			this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));  
			this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));
			this.outBuffer = new ArrayList<OutBuffer>();
			this.clientSocket = clientSocket;
			//GHD: Set socket read timeout and keep alive flags
			this.clientSocket.setKeepAlive(true);
			this.clientSocket.setSoTimeout(Constants.BITBOX_SOCKET_TIMEOUT);
			
			this.networkObserver = connectionManager;
			//connection ip/port
			this.peer = new HostPort(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
			this.peerSource = source;
			log.info("Active Connection maintained for: "+peer.toString());
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
				
	}
	
	/**
	 * added in case we need to temporarily stop the thread from running
	 */
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
				if (!this.clientSocket.isClosed() & this.clientSocket.isConnected())
				{
					if (this.outBuffer.size()>0) {
						OutBuffer buffer = outBuffer.remove(0);
						String outString = buffer.outData;
						log.info("outBuffer content to send: "+outString);
	    				out.write(outString);
	    				out.flush();
	    				//If this flag is set, we should terminate the connection after sending the last message
	    				if (buffer.terminateAfterSend)
	    				{
	    					log.info("terminateAfterSend flag set... closing the connection with "+peer.toString());
	    					closeAndClean();
	    				}
					}
					
					String inBuffer=receive();
					if (inBuffer!=null)
					{
						try {
							log.info("inBuffer content received: "+inBuffer);
							networkObserver.messageReceived(peer, inBuffer); //add to inQueue that is used by Event Processor
						//this.connectionManager.enqueueMessage(inBuffer);
						}
						catch(Exception e) {
							log.severe("unexpected exception in message receive "+e.getMessage()+" message dropped");
							e.printStackTrace();
						}
					}

					Thread.sleep(Constants.BITBOX_THREAD_SLEEP_TIME);
				}
				else
				{
					log.info("clientSocket Closed or not connected anymore... closing the connection with "+peer.toString());
					closeAndClean();
				}
			}
		}
		
		
		catch (Exception e)
		{
			//TODO should raise connection close event to the connection manager to remove this from the list
			//networkObserver.connectionClosed(peer);
			
			//close the socket and cleanup
			log.severe("exception in connection thread "+e.getMessage()+" closing the connection with peer "+peer.toString());
			closeAndClean();
			e.printStackTrace();
		}
	}
	
	private void closeAndClean()
	{
		try {
			this.running = false;
			this.networkObserver.connectionClosed(peer);
			if (this.clientSocket!=null && !clientSocket.isClosed()) this.clientSocket.close();
			if (in != null) in.close();
			if (out!= null) out.close();
		}catch(Exception e) {
			log.severe("exception while close & clean connection "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Fills connection outgoing buffer with data to be sent to 
	 * @param message
	 */
	
	
	//GHD: maybe sunchronized is required here
	public /*synchronized*/ void send(String message, boolean terminateAfterSend)
	{
		log.info(String.format("In connection.send, Peer: %s message: %s", peer.toString(),message));
		this.outBuffer.add(new OutBuffer(message, terminateAfterSend));
	}
	
	public String receive()
	{
		String inBuffer = null;
		try
		{
			inBuffer = this.in.readLine();
		}
		catch (SocketException e)
		{
			networkObserver.connectionClosed(peer);
			this.running = false;
		}
		catch(SocketTimeoutException ex)
		{
			//Do nothing, this is expected if no message arrived within the socket timeout set
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return inBuffer;
	}
	
	class OutBuffer
	{
		String outData;
		boolean terminateAfterSend = false;
		public OutBuffer(String data, boolean terminateAfter) {
			this.outData = data;
			this.terminateAfterSend = terminateAfter;
		}
	}

}
