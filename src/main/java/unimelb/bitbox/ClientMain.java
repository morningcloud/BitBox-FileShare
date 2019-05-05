package unimelb.bitbox;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
//import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class ClientMain {

	private Queue<HostPort> globalBFSQueue; 
	public ConnectionManager connectionManager;
	

    public ClientMain(ConnectionManager connectionManager) {
    	this.globalBFSQueue  = new LinkedList<HostPort>(); 
        this.connectConfigPeers(Configuration.getConfigurationValue("peers"), this);
		this.connectionManager = connectionManager;
    }
    
    private void connectConfigPeers(String list, ClientMain peer){
    	String[] hostList = list.split(",");
    	for (String h: hostList) 
    	{
    		PeerRunnable p1 = new PeerRunnable(new HostPort(h),peer);    		
    		p1.start();
    		
    	}
    }

	synchronized public Queue<HostPort> getGlobalBFSQueue() {
		return globalBFSQueue;
	}
	
	private class PeerRunnable extends Thread {		
		 
		private HostPort pHostPort;

		private BufferedReader in;
		private BufferedWriter out;
		private Logger log = Logger.getLogger(PeerRunnable.class.getName());
		//private Protocol protocol;
		private ClientMain peer;
		Socket socket = null;
		
		public PeerRunnable(HostPort pHostPort, ClientMain peer){
			this.pHostPort = pHostPort;
			//this.protocol  = new Protocol();
			this.peer = peer;
			
		}		

		public void run() {
			// System.out.println("Peer Thread Started");
	    	
	    	boolean connected = false;    	
	    	Document rxMsg = new Document();
	    	int timer = 1;
	    		/* thread waits for 100msec after each try
	    		 * so timer = 10 will make 10sec of trying to
	    		 * connect each peer.
	    		 */
	    	//Before attempting connection verify if this peer is already having active connection with us
	    	connected = connectionManager.isPeerConnected(pHostPort);
	    		while (!connected && timer<=Constants.BITBOX_CONNECTION_ATTEMPT_MAX_COUNT){ 	
	    			//System.out.println("Timer: "+ timer);
	    				log.warning(this.getName()+ ":"+String.format("Trying peer=%s:%s...\n" , pHostPort.host , pHostPort.port));
	    				try	{
	    					socket = new Socket(pHostPort.host,pHostPort.port);// an object is only assigned to socket if a connection is established   					 
	    					
	        				if (socket.isConnected() && !socket.isClosed()){
	            				socket.setKeepAlive(true);
	            				socket.setSoTimeout(Constants.BITBOX_INCOMING_SOCKET_TIMEOUT);
	            				
	            				connected = true;
	            				log.info(this.getName()+ ":"+"Socket connected to peer "
	            						+ pHostPort.host +":"+ pHostPort.port );
	            				log.info(this.getName() +"Local socket address: "+ socket.getLocalSocketAddress());

	            			}
	    				}
	    				catch (UnknownHostException e)
	    				{
	    					log.warning(this.getName()+ ":"+"unknown host " 
	    	    					+ pHostPort.host + ":" + pHostPort.port);
	    				}
	    				catch (ConnectException e){// Failed to connect to socket 
	    					log.severe(this.getName()+ ":"+"Connection Exception in attempt to " 
	    							+ pHostPort.host + ":" + pHostPort.port + " failed.");    
	    				}catch (NullPointerException m){
	    	    			log.warning(this.getName()+ ":"+"socket is not open, " 
	    	    					+ pHostPort.host + ":" + pHostPort.port);
	    	    		}catch (Exception z)	{
	    	    			z.printStackTrace();
	    	    			log.warning(this.getName()+ ":"+z.getMessage() 
	    	    					+ pHostPort.host + ":" + pHostPort.port);
	    	    		}
	    				
	    				// try to establish new Socket Connection after 100ms
	    	    		try {
	    					Thread.sleep(Constants.BITBOX_CONNECTION_THREAD_SLEEP_TIME);
	    				} catch (InterruptedException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
	    	    		timer++; //thread wait for 100msec so count if timer =100 will make 10sec
	    		}
	    		
	    		
	    		if(socket!=null) {
	    			
		    		if (socket.isConnected() && !socket.isClosed()){//if connected to a peer before time out...
		    			try{
		    					in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));  
		    					out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
				        		log.warning(this.getName()+ ":"+"Client Sending Handshake Request to "+pHostPort.toString());
				        		String hsr = Protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,
				        				null);
				        		out.write(hsr);
								out.flush();								 
				        					        		
				        		log.info(this.getName()+ ":"+"Client Sent Handshake_Request to "+pHostPort.toString()+ " message sent: " +hsr);
				        		rxMsg =  Document.parse(in.readLine());
				        		
				        		//TODO remove the following line, just for debugging
				        		//System.out.println(rxMsg.toJson());
				        		log.info(this.getName()+ ":"+"Client Received response to Handshake_Request from "+pHostPort.toString()+" message received: "+rxMsg.toJson());
				        			
				        		if (Protocol.validateHSRefused(rxMsg)){
				        			
				        			ArrayList<Document> peersHostPort = (ArrayList<Document>) rxMsg.get("peers");
				        			int numOfPeersReceived=0;
				        			for(Document hostPort:peersHostPort) 
				        			{
				        				peer.getGlobalBFSQueue().add(new HostPort(hostPort));
				        				numOfPeersReceived++;
				        			}
				        			log.warning(this.getName()+ 
				        					String.format("Connection refused from %s. Received %s peer(s).\n", pHostPort.toString(), numOfPeersReceived));
				        			closeSocket(out,in,socket);
				        			BFSNextPeer();
				        				
				        		}        					
				        		else if (Protocol.validateHSResponse(rxMsg)){
				        			Document hostPort = (Document) rxMsg.get("hostPort");
				        			log.info("Connection established to " + hostPort.getString("host") +
				        					" at port " + hostPort.getLong("port"));
				        			
				        			connectionManager.addConnection(socket, PeerSource.CLIENT, new HostPort(pHostPort.host,pHostPort.port));
				        		}else {
				        				
				        				log.warning(String.format("Invalid message received: <%s>.\n",rxMsg));
				        				out.write(Protocol.createMessage(Constants.Command.INVALID_PROTOCOL,"message invalid".split(":")));
				        				out.flush();
				        				closeSocket(out,in,socket);
				        				log.warning(this.getName()+ ":"+"Message_Invalid: Connection to this peer terminated");
				        				//Ask team if a new connection needed after message invalid
				        				BFSNextPeer();
				        			}
			    			}
						catch (SocketTimeoutException e)
						{
							if (socket!=null)
							{
								log.warning(String.format("Socket timeout while waiting for Handshake Response from %s dropping the connection.",
										socket.getInetAddress().getHostName()));
								
								closeSocket(out,in,socket);
			        			BFSNextPeer();
							}
						}
				    	catch (NullPointerException m){
				    		m.printStackTrace();
				    		log.severe(this.getName()+ ":"+m.getMessage() + ": Null Pointer Exception during message processing");
				    	}
		    			catch (IOException m){
		    				m.printStackTrace();
				    		log.severe(this.getName()+ ":"+m.getMessage() + ": IOException during message processing");
		    			}
				    	catch (Exception z){
				    		z.printStackTrace();
				    		log.severe(this.getName()+ " Exception during message processing : "+z.getMessage());
				    	}
		    		}
		}
	    		else { // if not connected to a peer after timer expiry.
	    			log.warning(this.getName()+ ":Trying next Peer");
	    			BFSNextPeer();	
		    		
	    		}
		}
		
		private void BFSNextPeer() {
			
			if(!peer.getGlobalBFSQueue().isEmpty())
			{
				PeerRunnable p1 = new PeerRunnable((HostPort) peer.getGlobalBFSQueue().poll(),this.peer);		
	    		p1.start();
			}
			else {
				log.severe(this.getName()+" : "+"Thread has failed to find any peer to connect");
				closeSocket(out,in,socket);
			}
		}

		private void closeSocket(BufferedWriter out,BufferedReader in,Socket socket) {
			try	{
				if(out!=null)    out.close();
				if(in!=null)     in.close();
				if(socket!=null) {
					socket.close();				
					log.warning(this.getName()+ ":"+"Socket Closed");
				}else {
					log.warning(this.getName()+ ":"+"Socket is null when trying to close");
				}
			}catch(IOException e){
				log.severe(this.getName()+ ":"+e.getMessage());
				e.printStackTrace();
			}		
		}
	}
		
   
}