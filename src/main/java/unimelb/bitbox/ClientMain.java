package unimelb.bitbox;

import java.net.ConnectException;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class ClientMain {

	private Queue<HostPort> globalBFSQueue; 
	public ConnectionManager connectionManager;
	
    public static void main(String[] args) {
    	//For Testing
    	LinkedBlockingQueue<Message> incomingMessagesQ = new LinkedBlockingQueue<>();
		String serverName = Configuration.getConfigurationValue("advertisedName");
		int serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
    	HostPort serverHostPort = new HostPort(serverName, serverPort);
    	
    	ConnectionManager connectionManager = new ConnectionManager(5, serverHostPort, incomingMessagesQ);
        ClientMain agent = new ClientMain(connectionManager);        
    	
    }
    

    public ClientMain(ConnectionManager connectionManager) {
    	this.globalBFSQueue  = new LinkedList<HostPort>(); 
        this.connectConfigPeers(Configuration.getConfigurationValue("peers"), this);
		this.connectionManager = connectionManager;
    }
    
    private void connectConfigPeers(String list, ClientMain peer){
    	System.out.println("here");
    	String[] hostList = list.split(",");
    	for (String h: hostList) {
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
		private Protocol protocol;
		private ClientMain peer;
		Socket socket = null;
		
		public PeerRunnable(HostPort pHostPort, ClientMain peer){
			this.pHostPort = pHostPort;
			this.protocol  = new Protocol();
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
	    			System.out.println("Timer: "+ timer);
	    				log.warning(this.getName()+ ":"+"Trying peer=%s:%s..." + pHostPort.host + pHostPort.port);
	    				try	{
	        				socket = new Socket(pHostPort.host,pHostPort.port);// an object is only assigned to socket if a connection is established   					 
	            			if (socket.isConnected() && !socket.isClosed()){
	            				socket.setKeepAlive(true);
	            				
	            				connected = true;
	            				log.warning(this.getName()+ ":"+"Socket Connected to peer "
	            						+ pHostPort.host + pHostPort.port );
	            				log.warning(this.getName() + socket.getLocalSocketAddress());

	            			}
	    				}catch (ConnectException e){// Failed to connect to socket 
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
	    					Thread.sleep(100);
	    				} catch (InterruptedException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
	    	    		timer++; //thread wait for 100msec so count if timer =100 will make 10sec
	    		}
	    		
	    		
	    		if(socket!=null) {
	    			log.warning(this.getName()+ ":"+"Is Client Socket Connected: " + socket.isConnected());
		    		log.severe(this.getName()+ ":"+"Is Cleint Socket Closed: " +socket.isClosed());
		    		
		    		if (socket.isConnected() && !socket.isClosed()){//if connected to a peer before time out...
		    			try{
		    					log.warning(this.getName()+ ":"+"Trying to initiate Handshake_Request");	
		    					in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));  
		    					out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
				        		log.warning(this.getName()+ ":"+"Cient Input and Output buffers Successfully Initiated");	
				        		log.warning(this.getName()+ ":"+"Client Sending Handshake_Request");
				        		String hsr = Protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,
				        				socket.getLocalSocketAddress().toString().split(":"));
								 out.write(hsr);
								 out.flush();								 
				        					        		
				        		log.warning(this.getName()+ ":"+"Client Sent Handshake_Request");
				        		rxMsg =  Document.parse(in.readLine());
				        		System.out.println(rxMsg.toJson());
				        		log.warning(this.getName()+ ":"+"Client Received Handshake_response");
				        			
				        		if (Protocol.validateHSRefused(rxMsg)){
				        			ArrayList<Document> peersHostPort = (ArrayList<Document>) rxMsg.get("peers");
				        			for(Document hostPort:peersHostPort) peer.getGlobalBFSQueue().add(new HostPort(hostPort));
				        			closeSocket(out,in,socket);
				        			BFSNextPeer();
				        				
				        		}        					
				        		else if (Protocol.validateHSResponse(rxMsg)){
				        			Document hostPort = (Document) rxMsg.get("hostPort");
				        			System.out.println("BitBox connected to " + hostPort.getString("host") +
				        					" at port " + hostPort.getLong("port") + " for asynchronous communication" );
				        			
				        			connectionManager.addConnection(socket);
				        		}else {
				        				
				        				System.out.println("message invalid"); 
				        				out.write(this.protocol.createMessage(Constants.Command.INVALID_PROTOCOL,null));
				        				out.flush();
				        				//closeSocket(out,in,socket);
				        				log.warning(this.getName()+ ":"+"Message_Invalid: Connection to this peer terminated");
				        				//Ask team if a new connection needed after message invalid
				        				BFSNextPeer();
				        			}
			    			}
			    			
				    	catch (NullPointerException m){
				    		
				    			log.warning(this.getName()+ ":"+m.getMessage() + ": Null Pointer Exception during message processing");
				    	}
		    			catch (IOException m){
				    		
				    			log.warning(this.getName()+ ":"+m.getMessage() + ": IOException during message processing");
		    			}
				    	catch (Exception z){
				    			log.warning(this.getName()+ " Exception during message processing : "+z.getMessage());
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
				new PeerRunnable((HostPort) peer.getGlobalBFSQueue(),this.peer);
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
					log.warning(this.getName()+ ":"+"Socket found Null when trying to close");
				}
			}catch(IOException e){
				log.warning(this.getName()+ ":"+e.getMessage());
			}		
		}
	}
		
   
}