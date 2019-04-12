package unimelb.bitbox;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import java.net.*;
import java.io.*;

public class Peer 
{
	
	private static Logger log = Logger.getLogger(Peer.class.getName());
	// private ArrayList<HostPort> peerList = new ArrayList<HostPort>();
	
	/**
	 * serverName is the advertised name of
	 * this peer. Other peers will use it to
	 * connect to it. It is not supposed to
	 * change once read from configuration file
	 */
	static final public String serverName = 
			Configuration.getConfigurationValue("advertisedName");
	
	/**
	 * serverPort is the advertised port of
	 * this peer. Other peers will use it to
	 * connect to it. It is not supposed to
	 * change once read from configuration file
	 */
	static final int serverPort = 
			Integer.parseInt(Configuration.getConfigurationValue("port"));
	
	private Queue<HostPort> globalBFSQueue; 
	
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Skeleton code - Begin
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();        
        //Peer agent = new Peer();        
        //agent.connectConfigPeers(Configuration.getConfigurationValue("peers"), agent);
    	new ServerMain(Peer.serverName,Peer.serverPort);
    	
    	
        
    }
    
    
    public Peer() {
    	this.globalBFSQueue  = new LinkedList<HostPort>(); 
    }
    
    private void connectConfigPeers(String list, Peer peer){
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
		private Peer peer;
		
		public PeerRunnable(HostPort pHostPort, Peer peer){
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
	    				log.warning("Trying peer=%s:%s..." + pHostPort.host + pHostPort.port);
	    				try	{
	        				socket = new Socket(pHostPort.host,pHostPort.port);
	            			if (socket.isConnected()){
	            				socket.setKeepAlive(true);
	            				System.out.println("Socket Connected to peer "
	            						+ pHostPort.host + pHostPort.port );
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
	    		    		closeSocket(out,in,socket);
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
			        			
			        			log.warning("Sending Handshake_Request");
			        			out.writeUTF(this.protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,null));
			        			rxMsg =  Document.parse(in.readUTF());
			        			
			        			if (Protocol.validateHSRefused(rxMsg)){
			        				ArrayList<Document> peersHostPort = (ArrayList<Document>) rxMsg.get("peers");
			        				for(Document hostPort:peersHostPort) peer.getGlobalBFSQueue().add(new HostPort(hostPort));
			        				closeSocket(out,in,socket);
			        				BFSNextPeer();
			        				
			        				}        					
			        			}else if (Protocol.validateHSResponse(rxMsg)){
			        				Document hostPort = (Document) rxMsg.get("hostPort");
			        				System.out.println("BitBox connected to " + hostPort.getString("host") +
			        						" at port " + hostPort.getLong("port") + " for asynchronous communication" );
			        				
			        			}else
			        				System.out.println("message invalid"); 
			        				out.writeUTF(this.protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST,null));
			        				closeSocket(out,in,socket);
			        				log.warning("Message_Invalid: Connection to this peer terminated");
			        				//Ask team if a new connection needed after message invalid
			        				BFSNextPeer();
		    			}			
		    		
		    		catch (NullPointerException m){
		    				log.warning(m.getMessage() + "NullPointerException");
		    			}
		    		catch (Exception z){
		    				log.warning(z.getMessage());
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
