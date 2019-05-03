package unimelb.bitbox;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
	
    //TODO I'm commenting this whole section, it's never used.
	/*
	public static void main(String[] args) {
    	//For Testing
    	LinkedBlockingQueue<Message> incomingMessagesQ = new LinkedBlockingQueue<>();
		String serverName = Configuration.getConfigurationValue("advertisedName");
		int serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
    	HostPort serverHostPort = new HostPort(serverName, serverPort);
    	
    	ConnectionManager connectionManager = new ConnectionManager(5, serverHostPort, incomingMessagesQ);
        //ClientMain agent = new ClientMain(connectionManager);        
    	
    }
    */
    

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
	    		while (!connected && timer<=5){ 	
	    			//System.out.println("Timer: "+ timer);
	    				log.warning(this.getName()+ ":"+String.format("Trying peer=%s:%s...\n" , pHostPort.host , pHostPort.port));
	    				try	{
	    					socket = new Socket(pHostPort.host,pHostPort.port);// an object is only assigned to socket if a connection is established   					 
	    					
	        				if (socket.isConnected() && !socket.isClosed()){
	            				socket.setKeepAlive(true);
	            				
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
	    			//TODO remove the following, only valid for debugging.
	    			//log.warning(this.getName()+ ":"+"Is Client Socket Connected: " + socket.isConnected());
		    		//log.severe(this.getName()+ ":"+"Is Cleint Socket Closed: " +socket.isClosed());
		    		
		    		if (socket.isConnected() && !socket.isClosed()){//if connected to a peer before time out...
		    			try{
		    					//TODO remove the following logging in the final version.
		    					//log.warning(this.getName()+ ":"+"Trying to initiate Handshake_Request");	
		    					in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));  
		    					out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
				        		//TODO remove the following logging in the final version.
		    					//log.warning(this.getName()+ ":"+"Client Input and Output buffers Successfully Initiated");	
				        		log.warning(this.getName()+ ":"+"Client Sending Handshake Request to "+pHostPort.toString());
				        		String hsr = Protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,
				        				null);
				        		// TODO Remove this one. Just testing socket timeout.
				        		//System.out.println("Client.hsr="+hsr); 
								//Thread.sleep(1000);
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
				        				//closeSocket(out,in,socket);
				        				log.warning(this.getName()+ ":"+"Message_Invalid: Connection to this peer terminated");
				        				//Ask team if a new connection needed after message invalid
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