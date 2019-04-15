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
		private DataInputStream in;
		private DataOutputStream out;
		private Logger log = Logger.getLogger(PeerRunnable.class.getName());
		private Protocol protocol;
		private ClientMain peer;
		
		public PeerRunnable(HostPort pHostPort, ClientMain peer){
			this.pHostPort = pHostPort;
			this.protocol  = new Protocol();
			this.peer = peer;
			
		}		

		public void run() {
			// System.out.println("Peer Thread Started");
	    	Socket socket =null;
	    	boolean connected = false;    	
	    	Document rxMsg = new Document();
	    	int timer = 1;
	    		/* thread waits for 100msec after each try
	    		 * so timer = 10 will make 10sec of trying to
	    		 * connect each peer.
	    		 */
	    		while (!connected && timer<=5){ 	
	    			System.out.println("Timer: "+ timer);
	    				log.info(String.format("Trying peer=%s:%s..." , pHostPort.host , pHostPort.port));
	    				try	{
	        				socket = new Socket(pHostPort.host,pHostPort.port);
	            			if (socket.isConnected()){
	            				socket.setKeepAlive(true);
	            				System.out.println("Socket Connected to peer "
	            						+ pHostPort.host + ":" + pHostPort.port );
	            				connected = true;
	            				// after that a thread must be started to keep monitoring if
	            				// socket is alive or not. In case it is disconnected , it should
	            				// retry for some time (10s) afterwards a new connect request
	            				// to other peers in queue must be sent.
	            			}
	    				}catch (ConnectException e){// Failed to connect to socket 
	    					log.severe("Connection attempt to " 
	    							+ pHostPort.host + ":" + pHostPort.port + " failed.");    					    					
	    				}catch (NullPointerException m){
	    	    			log.warning("socket is not open, " 
	    	    					+ pHostPort.host + ":" + pHostPort.port);
	    	    		}catch (Exception z)	{
	    	    			log.warning(z.getMessage() 
	    	    					+ pHostPort.host + ":" + pHostPort.port);
	    	    		}finally	{
	    	    			//commented as closing the socket at this stage will break the handshake attempt below
	    	    			//this should be added in the exception blocks
	    		    		//closeSocket(out,in,socket);
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
	    		//if connected to a peer before time out...
	    		if(connected) {
	    			try{
		    			if (socket.isConnected()){
			    				in =  new DataInputStream(socket.getInputStream());
			        			out = new DataOutputStream(socket.getOutputStream());  			
			        			
			        			//out.writeUTF(this.protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,null));
			        			//Just put temporarly to test handshakes at least!
			        	        Document doc = new Document();
			        	        doc.append("command", Constants.Command.HANDSHAKE_REQUEST.name());
	        	                Document pd1 = new  Document();
	        		            pd1.append("host", pHostPort.host);
	        		            pd1.append("port", pHostPort.port);
	        		            doc.append("hostPort", pd1);
	        		            log.info("Sending to "+pHostPort.toString()+" message: "+doc.toJson());
			        			out.writeUTF(doc.toJson());

								Message inMsg = new Message(doc);
								inMsg.setFromAddress(pHostPort);
								connectionManager.getIncomingMessagesQueue().put(inMsg);
	        		            ///////////////End of Test code////////////
								rxMsg =  Document.parse(in.readUTF());
			        			
			        			if (Protocol.validateHSRefused(rxMsg)){
			        				ArrayList<Document> peersHostPort = (ArrayList<Document>) rxMsg.get("peers");
			        				for(Document hostPort:peersHostPort) peer.getGlobalBFSQueue().add(new HostPort(hostPort));
			        				closeSocket(out,in,socket);
			        				BFSNextPeer();
			        				
			        				      					
			        			}else if (Protocol.validateHSResponse(rxMsg)){
			        				Document hostPort = (Document) rxMsg.get("hostPort");
			        				log.info("Success! BitBox connected to " + hostPort.getString("host") +
			        						" at port " + hostPort.getLong("port") + " for asynchronous communication." );
			        				
			        				//Since handshake is successful add the socket to the active connection list for further communications
		            				connectionManager.addConnection(socket);
			        			}else
			        				System.out.println("message invalid"); 
			        				out.writeUTF(this.protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,null));
			        				closeSocket(out,in,socket);
			        				log.warning("Message_Invalid: Connection to this peer terminated");
			        				//Ask team if a new connection needed after message invalid
			        				BFSNextPeer();
		    			}		
	    			}
		    		catch (NullPointerException m){
		    				log.warning(m.getMessage() + "NullPointerException");
		    				m.printStackTrace();
		    			}
		    		catch (Exception z){
	    					log.severe(z.getMessage() + "Exception");
		    				z.printStackTrace();
		    			}
	    		}else // if not connected to a peer after timer expiry.
	    			BFSNextPeer();
		}
		
		private void BFSNextPeer() {
			if(!peer.getGlobalBFSQueue().isEmpty())
				new PeerRunnable((HostPort) peer.getGlobalBFSQueue(),this.peer);
			else 
				System.out.println("Thread has failed to find any peer to connect");			
		}

		private void closeSocket(DataOutputStream out,DataInputStream in,Socket socket) {
			try	{
				if(out!=null)    out.close();
				if(in!=null)     in.close();
				if(socket!=null) socket.close();
					
			}catch(IOException e){
				log.warning(e.getMessage());
			}		
		}
	}
		
}
